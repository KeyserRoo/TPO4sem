import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class Tools {

  public static Options createOptionsFromYaml(String fileName) {
    try (FileInputStream fis = new FileInputStream(fileName)) {
      Map<String, Object> data = new Yaml().load(fis);

      return new Options(
          (String) data.get("host"),
          (int) data.get("port"),
          (Boolean) data.get("concurMode"),
          (boolean) data.get("showSendRes"),
          (Map<String, List<String>>) data.get("clientsMap"));

    } catch (Exception e) {
      return null;
    }
  }
}
