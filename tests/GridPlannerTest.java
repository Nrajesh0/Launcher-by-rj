import dev.dothome.GridPlanner;
import java.util.*;

public class GridPlannerTest {
  public static void main(String[] args) {
    Random random = new Random(42);
    for (int columns = 4; columns <= 5; columns++)
      for (int trial = 0; trial < 200; trial++) {
        List<int[]> sizes = new ArrayList<>();
        for (int i = 0; i < 60; i++)
          sizes.add(new int[] {1 + random.nextInt(5), 1 + random.nextInt(3)});
        List<int[]> placed = GridPlanner.place(sizes, columns);
        Set<String> occupied = new HashSet<>();
        for (int[] p : placed) {
          if (p[0] < 0 || p[0] + p[2] > columns) throw new AssertionError("Out of bounds");
          for (int y = p[1]; y < p[1] + p[3]; y++)
            for (int x = p[0]; x < p[0] + p[2]; x++)
              if (!occupied.add(x + ":" + y)) throw new AssertionError("Overlap");
        }
        List<int[]> again = GridPlanner.place(sizes, columns);
        for (int i = 0; i < again.size(); i++)
          if (!Arrays.equals(placed.get(i), again.get(i)))
            throw new AssertionError("Unstable layout");
      }
    System.out.println("400 random layouts: bounds, collision and determinism checks passed");
  }
}
