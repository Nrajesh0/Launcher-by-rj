import dev.dothome.WorkspaceModel;
import java.util.*;
import org.json.*;

public class BatchMoveTest {
  static JSONObject item(String id, int page, int x, int y, int w, int h) {
    return new JSONObject()
        .put("id", id)
        .put("type", "clock")
        .put("page", page)
        .put("x", x)
        .put("y", y)
        .put("w", w)
        .put("h", h);
  }

  public static void main(String[] args) throws Exception {
    JSONArray original =
        new JSONArray()
            .put(item("a", 0, 0, 0, 2, 2))
            .put(item("b", 0, 2, 1, 1, 1))
            .put(item("fixed", 1, 3, 5, 1, 1));
    String before = original.toString();
    Set<String> selected = new HashSet<>(Arrays.asList("a", "b"));
    JSONArray moved = WorkspaceModel.moveGroup(original, selected, "a", 1, 0, 2, 4, 7);
    if (!original.toString().equals(before))
      throw new AssertionError("Move mutated original transaction");
    if (moved.getJSONObject(0).getInt("page") != 1 || moved.getJSONObject(1).getInt("y") != 3)
      throw new AssertionError("Relative arrangement lost");
    for (int[] target : new int[][] {{1, 2, 4}, {1, 0, 6}, {-1, 0, 0}}) {
      try {
        WorkspaceModel.moveGroup(original, selected, "a", target[0], target[1], target[2], 4, 7);
        throw new AssertionError("Invalid group accepted");
      } catch (JSONException expected) {
      }
      if (!before.equals(original.toString()))
        throw new AssertionError("Rejected move damaged original");
    }
    selected.add("missing");
    try {
      WorkspaceModel.moveGroup(original, selected, "a", 1, 0, 0, 4, 7);
      throw new AssertionError("Stale selection accepted");
    } catch (JSONException expected) {
    }
    System.out.println(
        "Atomic multi-size group moves, relative placement, bounds, collisions and stale-selection"
            + " rollback passed");
  }
}
