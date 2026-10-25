import dev.dothome.LayoutValidator;
import org.json.*;

public class LayoutValidatorTest {
  static JSONObject valid() {
    return new JSONObject(
        "{\"schema\":1,\"settings\":{\"columns\":4},\"items\":[{\"id\":\"a\",\"type\":\"clock\",\"w\":4,\"h\":2,\"page\":0}],\"dock\":[]}");
  }

  interface Mutation {
    void run(JSONObject x);
  }

  static void rejects(Mutation mutation) throws Exception {
    JSONObject x = valid();
    mutation.run(x);
    try {
      LayoutValidator.validate(x);
    } catch (Exception expected) {
      return;
    }
    throw new AssertionError("Invalid backup accepted: " + x);
  }

  public static void main(String[] a) throws Exception {
    LayoutValidator.validate(valid());
    rejects(x -> x.put("schema", 99));
    rejects(x -> x.getJSONObject("settings").put("columns", 100));
    rejects(x -> x.getJSONObject("settings").put("iconSize", -4));
    rejects(x -> x.getJSONArray("items").getJSONObject(0).put("page", -1));
    rejects(x -> x.getJSONArray("items").getJSONObject(0).put("w", 0));
    rejects(x -> x.getJSONArray("items").getJSONObject(0).put("h", 100));
    rejects(x -> x.getJSONArray("items").getJSONObject(0).put("type", "unknown"));
    rejects(x -> x.getJSONArray("items").put(x.getJSONArray("items").getJSONObject(0)));
    rejects(
        x -> {
          JSONArray d = new JSONArray();
          for (int i = 0; i < 5; i++) d.put("app");
          x.put("dock", d);
        });
    rejects(
        x -> {
          for (int i = 0; i < 151; i++)
            x.getJSONArray("items").put(new JSONObject().put("id", "id" + i).put("type", "date"));
        });
    System.out.println("Valid layout and 10 malformed backup cases passed");
  }
}
