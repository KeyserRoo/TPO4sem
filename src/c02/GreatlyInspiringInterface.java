import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class GreatlyInspiringInterface {
  private JFrame mainFrame;
  private JFXPanel webViewPanel;
  private JPanel currencyPanel;
  private JPanel weatherPanel;

  public GreatlyInspiringInterface(Service service, String weatherJson, double rate1, double rate2) {
    initializeGUI(service, weatherJson, rate1, rate2);
  }

  public void display() {
    mainFrame.setVisible(true);
  }

  private void initializeGUI(Service service, String weatherJson, double rate1, double rate2) {
    JPanel topPanel = initializeTopPanel(service, weatherJson, rate1, rate2);
    JPanel bottomPanel = createBottomPanel(service);

    mainFrame = new JFrame(service.country + ", " + service.city);
    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setSize(1200, 800);
    mainFrame.add(topPanel, BorderLayout.NORTH);
    mainFrame.add(bottomPanel, BorderLayout.CENTER);
  }

  private JPanel initializeTopPanel(Service service, String weatherJson, double rate1, double rate2) {
    JPanel topPanel = new JPanel(new GridLayout(1, 2));
    setWeatherPanel(weatherJson);
    JPanel rightPanel = createRightPanel(service, rate1, rate2);
    topPanel.add(weatherPanel);
    topPanel.add(rightPanel);
    return topPanel;
  }

  private JPanel createBottomPanel(Service service) {
    JPanel bottomPanel = new JPanel(new BorderLayout());
    webViewPanel = new JFXPanel();
    bottomPanel.add(webViewPanel, BorderLayout.CENTER);
    updateWebEngine(service.country, service.city);
    return bottomPanel;
  }

  private String parseWeather(String weatherJson) {
    Weather weather = new Gson().fromJson(weatherJson, Weather.class);
    StringBuilder toReturn = new StringBuilder("");
    for (Map.Entry<String, String> item : weather.main.entrySet()) {
      toReturn.append(item.getKey() + "=" + item.getValue() + "\n");
    }
    return toReturn.toString();
  }

  private JPanel createRightPanel(Service service, double rate1, double rate2) {
    JPanel rightPanel = new JPanel(new GridLayout(1, 3));
    setCurrencyPanel(service, rate1, rate2);
    JPanel dataPanel = createDataPanel();
    JButton searchButton = new JButton("Search");

    searchButton.addActionListener(e -> {
      SwingUtilities.invokeLater(() -> {
        String country = ((JTextField) dataPanel.getComponent(0)).getText();
        String city = ((JTextField) dataPanel.getComponent(1)).getText();

        Service s = new Service(country);
        String weatherJson = s.getWeather(city);
        Double r1 = s.getRateFor("USD");
        Double r2 = s.getNBPRate();

        setWeatherPanel(weatherJson);
        setCurrencyPanel(s, r1, r2);
        updateWebEngine(country, city);

        weatherPanel.revalidate();
        weatherPanel.repaint();
        currencyPanel.revalidate();
        currencyPanel.repaint();
      });
    });

    rightPanel.add(currencyPanel);
    rightPanel.add(dataPanel);
    rightPanel.add(searchButton);
    return rightPanel;
  }

  private JPanel createDataPanel() {
    JPanel dataPanel = new JPanel(new GridLayout(2, 1));
    JTextField inputCountry = new JTextField();
    inputCountry.setBorder(BorderFactory.createTitledBorder("Country"));
    dataPanel.add(inputCountry);

    JTextField inputCity = new JTextField();
    inputCity.setBorder(BorderFactory.createTitledBorder("City"));
    dataPanel.add(inputCity);
    return dataPanel;
  }

  private void setWeatherPanel(String weatherJson) {
    if (weatherPanel == null) {
      weatherPanel = new JPanel(new GridLayout(2, 3));
    } else {
      weatherPanel.removeAll();
    }

    String[] weatherData = parseWeather(weatherJson).split("\n");
    for (int i = 0; i < 6; i++) {
      String[] item = weatherData[i].split("=");
      JLabel label = new JLabel(item[1]);
      label.setBorder(BorderFactory.createTitledBorder(item[0]));
      weatherPanel.add(label);
    }

    weatherPanel.invalidate();
    weatherPanel.revalidate();
    weatherPanel.repaint();
  }

  private void setCurrencyPanel(Service service, double rate1, double rate2) {
    if (currencyPanel == null) {
      currencyPanel = new JPanel(new GridLayout(2, 1));
    } else {
      currencyPanel.removeAll();
    }

    JLabel plnToCurrency = new JLabel(String.valueOf(rate1));
    plnToCurrency
        .setBorder(BorderFactory.createTitledBorder("1 " + service.gettingRateFor + " to " + service.countryCurrency));
    currencyPanel.add(plnToCurrency);

    JLabel currencyToPLN = new JLabel(String.valueOf(rate2));
    currencyToPLN.setBorder(BorderFactory.createTitledBorder("1 " + service.countryCurrency + " to PLN"));
    currencyPanel.add(currencyToPLN);

    currencyPanel.invalidate();
    currencyPanel.revalidate();
    currencyPanel.repaint();
  }

  private void updateWebEngine(String country, String city) {
    String url = "https://en.wikipedia.org/wiki/" + city;
    Platform.runLater(() -> {
      WebView browser = new WebView();
      WebEngine webEngine = browser.getEngine();
      webEngine.load(url);
      Scene scene = new Scene(browser);
      webViewPanel.setScene(scene);
    });
  }
}