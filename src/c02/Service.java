import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

public class Service {
  String country;
  String countryISO;
  String countryCurrency;
  String city;

  String gettingRateFor;

  String apiKeyWeather;
  String apiKeyOpenEr;

  public Service(String cou) {
    country = countryNameConverter(cou);
    initializeISOAndCurrency();

    Map<String, String> keys = KeysLoader.loadKeys();
    apiKeyOpenEr = keys.getOrDefault("apiopenerkey", "");
    apiKeyWeather = keys.getOrDefault("apiweatherkey", "");
  }

  public String getCurrency() {
    return countryCurrency;
  }

  public String getISO() {
    return countryISO;
  }

  public String getWeather(String ci) {
    city = ci;
    String url = String.format(
        "http://api.openweathermap.org/data/2.5/weather?q=%s,%s&appid=%s&units=metric", country, ci, apiKeyWeather);
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new URI(url).toURL().openConnection().getInputStream()))) {

      String json = reader.lines().collect(Collectors.joining());
      return json;

    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

  public Double getRateFor(String currency) {
    gettingRateFor = currency;
    String url = String.format(
        "https://v6.exchangerate-api.com/v6/%s/latest/%s", apiKeyOpenEr, gettingRateFor);
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new URI(url).toURL().openConnection().getInputStream()))) {

      String json = reader.lines().collect(Collectors.joining());
      Money money = new Gson().fromJson(json, Money.class);
      return Double.parseDouble(money.conversion_rates.get(countryCurrency));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1.0;

  }

  public Double getNBPRate() {
    if (countryISO.equals("PL"))
      return 1.0;

    double toRet = -1;
    toRet = odwiedzNBP("http://api.nbp.pl/api/exchangerates/rates/a/" + countryCurrency + "/?format=json");
    if (toRet == -1)
      toRet = odwiedzNBP("http://api.nbp.pl/api/exchangerates/rates/b/" + countryCurrency + "/?format=json");

    return toRet;
  }

  private double odwiedzNBP(String path) {
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new URI(path).toURL().openConnection().getInputStream()))) {
      String json = reader.lines().collect(Collectors.joining());
      return JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("rates").get(0).getAsJsonObject().get("mid")
          .getAsDouble();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }

  private void initializeISOAndCurrency() {
    for (Locale locale : Locale.getAvailableLocales()) {
      if (locale.getDisplayCountry(Locale.ENGLISH).equalsIgnoreCase(country)) {
        countryISO = locale.getCountry();
        countryCurrency = Currency.getInstance(locale).getCurrencyCode();
      }
    }
  }

  public static String countryNameConverter(String country) {
    for (Locale locale1 : Locale.getAvailableLocales()) {
      for (Locale locale2 : Locale.getAvailableLocales()) {
        if (!locale1.getCountry().isEmpty() && locale1.getDisplayCountry(locale2).equalsIgnoreCase(country)) {
          return locale1.getDisplayCountry(Locale.ENGLISH);
        }
      }
    }
    return "";
  }
}

class Weather {
  Map<String, String> main = new HashMap<>();
}

class Money {
  Map<String, String> conversion_rates = new HashMap<>();
}

class KeysLoader {
  private static final String KEYS_FILE_PATH = "./src/c02/.keys.txt";

  public static Map<String, String> loadKeys() {
    Map<String, String> keys = new HashMap<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(KEYS_FILE_PATH))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split(":");
        if (parts.length == 2) {
          keys.put(parts[0].trim(), parts[1].trim());
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return keys;
  }
}