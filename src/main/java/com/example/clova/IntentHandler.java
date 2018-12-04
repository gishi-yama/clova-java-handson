package com.example.clova;

import com.linecorp.clova.extension.boot.handler.annnotation.*;
import com.linecorp.clova.extension.boot.message.response.CEKResponse;
import com.linecorp.clova.extension.boot.message.speech.OutputSpeech;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.net.URI;


@CEKRequestHandler
public class IntentHandler {

  private static final Logger log = LoggerFactory.getLogger(IntentHandler.class);

  // スキル起動時の処理
  @LaunchMapping
  CEKResponse handleLaunch() {
    // Clovaに話させる。スキル(Session)は終了しない
    return CEKResponse.builder()
      .outputSpeech(OutputSpeech.text("○○の環境を教えて、と聞いてください。"))
      .shouldEndSession(false)
      .build();
  }

  // EnvIntent 発生時の処理。引数をスロット名と一致させること！
  @IntentMapping("EnvIntent")
  CEKResponse handleIntent(@SlotValue Optional<String> area) {
    // スロットタイプが聞き取れていれば callbackEnvメソッド を呼び出す
    // 聞き取れていなければ 聞き取れませんでした を結果にする
    String outputSpeechText = area
      .map(value -> callbackEnv(value))
      .orElse("聞き取れませんでした。");

    // Clovaに callbackEnv の結果を話させる。スキル(Session）は終了しない
    return CEKResponse.builder()
      .outputSpeech(OutputSpeech.text(outputSpeechText))
      .shouldEndSession(false)
      .build();
  }

  // 聞き取れたスロットタイプの内容から、回答を作る
  private String callbackEnv(String area) {
    switch (area) {
      case "食堂":
        // 変更箇所, "******" の実際の値はハンズオン時に提示する
        return replyRoomInfo("b48a373d768e6f54330a236a6b118137");
      case "学生ホール":
        return "二酸化炭素濃度は1500ppm、温度は25度です。";
      default:
        return "ごめんなさい、その場所は登録されていません。";
    }
  }

  // 「キャンセル」と言われたときの処理
  @IntentMapping("Clova.CancelIntent")
  CEKResponse handleCancelIntent() {
    // Clovaに話させる。スキル(Session)も終了する
    return CEKResponse.builder()
      .outputSpeech(OutputSpeech.text("部屋の環境スキルを終了します。"))
      .shouldEndSession(true)
      .build();
  }

  // スキルの終了時の処理
  @SessionEndedMapping
  CEKResponse handleSessionEnded() {
    log.info("部屋の環境スキルを終了しました。");
    return CEKResponse.empty();
  }

  // センサーの値をWebから取得して、CO2クラスのインスタンスにいれる
  private String replyRoomInfo(String key) {
    String url = "https://us.wio.seeed.io/v1/node/GroveCo2MhZ16UART0/concentration_and_temperature?access_token=";
    URI uri = URI.create(url + key);
    RestTemplate restTemplate = new RestTemplateBuilder().build();
    try {
      CO2 co2 = restTemplate.getForObject(uri, CO2.class);
      return "二酸化炭素は"
        + co2.getConcentration()
        + "ppm、温度は"
        + co2.getTemperature()
        + "度です";
    } catch (HttpClientErrorException e) {
      e.printStackTrace();
      return "センサーに接続できていません";
    }
  }

}