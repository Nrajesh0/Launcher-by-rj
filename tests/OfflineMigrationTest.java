import dev.dothome.OfflineMigration;
import org.json.*;
public class OfflineMigrationTest {
  public static void main(String[] args) throws Exception {
    JSONObject legacy = new JSONObject("{\"items\":[{\"id\":\"a\",\"type\":\"notes\"},{\"id\":\"b\",\"type\":\"weather\",\"lat\":12.0},{\"id\":\"c\",\"type\":\"clock\"}]}");
    OfflineMigration.apply(legacy);
    JSONArray items=legacy.getJSONArray("items");
    if(items.length()!=2 || !items.getJSONObject(0).getString("id").equals("a") || !items.getJSONObject(1).getString("id").equals("c")) throw new AssertionError("Migration lost or reordered offline tiles");
    String once=legacy.toString(); OfflineMigration.apply(legacy);
    if(!once.equals(legacy.toString())) throw new AssertionError("Migration is not idempotent");
    System.out.println("Offline upgrade/import migration passed");
  }
}
