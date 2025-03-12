import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class AStarVisualizer extends JPanel {
    // --- A* Search Algorithm with Height Constraints ---

    static class Cell {
        int parent_i, parent_j;
        double f, g, h;
        Cell() {
            parent_i = -1;
            parent_j = -1;
            f = Double.POSITIVE_INFINITY;
            g = Double.POSITIVE_INFINITY;
            h = Double.POSITIVE_INFINITY;
        }
    }

    // Returns a list of [row, col] coordinates for the path from src to dest,
    // or null if no path is found.
    static List<int[]> findPath(float[][] map, int[] src, int[] dest) {
        final int ROW = map.length;
        final int COL = map[0].length;

        if (!isValid(src[0], src[1], ROW, COL) || !isValid(dest[0], dest[1], ROW, COL)) {
            System.out.println("Source or destination is invalid");
            return null;
        }
        if (src[0] == dest[0] && src[1] == dest[1]) {
            System.out.println("Already at destination");
            List<int[]> path = new ArrayList<>();
            path.add(src);
            return path;
        }

        boolean[][] closedList = new boolean[ROW][COL];
        Cell[][] cellDetails = new Cell[ROW][COL];
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                cellDetails[i][j] = new Cell();
            }
        }

        int i = src[0], j = src[1];
        cellDetails[i][j].f = 0.0;
        cellDetails[i][j].g = 0.0;
        cellDetails[i][j].h = 0.0;
        cellDetails[i][j].parent_i = i;
        cellDetails[i][j].parent_j = j;

        // Use a map for the open list where keys are f-values.
        Map<Double, int[]> openList = new HashMap<>();
        openList.put(0.0, new int[] { i, j });

        while (!openList.isEmpty()) {
            // Find the cell in the open list with the smallest f-value.
            Map.Entry<Double, int[]> p = null;
            for (Map.Entry<Double, int[]> entry : openList.entrySet()) {
                if (p == null || entry.getKey() < p.getKey()) {
                    p = entry;
                }
            }
            if (p == null)
                break;

            openList.remove(p.getKey());
            i = p.getValue()[0];
            j = p.getValue()[1];
            closedList[i][j] = true;

            // Explore the 8 possible neighbors: N, S, E, W, NE, NW, SE, SW.
            int[][] directions = {
                    {-1, 0}, {1, 0}, {0, 1}, {0, -1},
            };

            for (int[] d : directions) {
                int newRow = i + d[0];
                int newCol = j + d[1];

                if (isValid(newRow, newCol, ROW, COL) && canMove(map, i, j, newRow, newCol)) {
                    // Destination reached.
                    if (newRow == dest[0] && newCol == dest[1]) {
                        cellDetails[newRow][newCol].parent_i = i;
                        cellDetails[newRow][newCol].parent_j = j;
                        return tracePath(cellDetails, dest);
                    }
                    if (!closedList[newRow][newCol]) {
                        double moveCost = 1.0;
                        double gNew = cellDetails[i][j].g + moveCost;
                        double hNew = calculateHValue(newRow, newCol, dest, map);
                        double fNew = gNew + hNew;

                        if (cellDetails[newRow][newCol].f == Double.POSITIVE_INFINITY || cellDetails[newRow][newCol].f > fNew) {
                            openList.put(fNew, new int[] { newRow, newCol });
                            cellDetails[newRow][newCol].f = fNew;
                            cellDetails[newRow][newCol].g = gNew;
                            cellDetails[newRow][newCol].h = hNew;
                            cellDetails[newRow][newCol].parent_i = i;
                            cellDetails[newRow][newCol].parent_j = j;
                        }
                    }
                }
            }
        }
        System.out.println("Path not found");
        return null;
    }

    static boolean isValid(int row, int col, int ROW, int COL) {
        return row >= 0 && row < ROW && col >= 0 && col < COL;
    }

    // Checks whether moving from (curRow,curCol) to (newRow,newCol) is allowed.
    // A move is permitted only if the new cell's height is not more than 0.25 higher.
    static boolean canMove(float[][] map, int curRow, int curCol, int newRow, int newCol) {
        float currentHeight = map[curRow][curCol];
        float newHeight = map[newRow][newCol];
        return (newHeight - currentHeight) <= 0.25f;
    }

    // 3D Euclidean distance heuristic (includes height difference).
    static double calculateHValue(int row, int col, int[] dest, float[][] map) {
        double dx = row - dest[0];
        double dy = col - dest[1];
        double dz = map[row][col] - map[dest[0]][dest[1]];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // Backtracks from the destination cell to the source to build the path.
    static List<int[]> tracePath(Cell[][] cellDetails, int[] dest) {
        List<int[]> path = new ArrayList<>();
        int row = dest[0];
        int col = dest[1];
        while (!(cellDetails[row][col].parent_i == row && cellDetails[row][col].parent_j == col)) {
            path.add(new int[] { row, col });
            int temp_row = cellDetails[row][col].parent_i;
            int temp_col = cellDetails[row][col].parent_j;
            row = temp_row;
            col = temp_col;
        }
        path.add(new int[] { row, col });
        Collections.reverse(path);
        return path;
    }

    // --- Visualization Variables & Constructor ---

    private float[][] map;
    private List<int[]> path;
    private int cellSize = 50; // pixel size for each grid cell

    public AStarVisualizer(float[][] map, List<int[]> path) {
        this.map = map;
        this.path = path;
        setPreferredSize(new Dimension(map[0].length * cellSize, map.length * cellSize));
    }

    // --- Drawing the Grid and Path ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int rows = map.length;
        int cols = map[0].length;

        // Determine min and max heights to normalize cell colors.
        float minHeight = Float.MAX_VALUE;
        float maxHeight = Float.MIN_VALUE;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (map[i][j] < minHeight)
                    minHeight = map[i][j];
                if (map[i][j] > maxHeight)
                    maxHeight = map[i][j];
            }
        }

        // Draw each cell with a color gradient based on its height.
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                float height = map[i][j];
                float normalized = (height - minHeight) / (maxHeight - minHeight);
                // Lower heights appear more blue, higher heights more red.
                Color cellColor = new Color(normalized, 0, 1 - normalized);
                g.setColor(cellColor);
                g.fillRect(j * cellSize, i * cellSize, cellSize, cellSize);
                g.setColor(Color.BLACK);
                g.drawRect(j * cellSize, i * cellSize, cellSize, cellSize);
                // Optionally, draw the height value.
                g.setColor(Color.WHITE);
                g.drawString(String.format("%.2f", height), j * cellSize + 5, i * cellSize + 15);
            }
        }

        // If a path was found, overlay it on the grid.
        if (path != null) {
            for (int k = 0; k < path.size(); k++) {
                int[] cell = path.get(k);
                int row = cell[0];
                int col = cell[1];
                // Semi-transparent green overlay for path cells.
                g.setColor(new Color(0, 255, 0, 128));
                g.fillRect(col * cellSize, row * cellSize, cellSize, cellSize);
                // Draw the step number in the cell.
                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(k), col * cellSize + cellSize / 2, row * cellSize + cellSize / 2);
            }
        }
    }



    // --- Main Method: Testing and Visualization ---
    public static void main(String[] args) {
        // Define a sample map (9 rows x 10 columns) with height values.
        double[][] floatMap = {
                {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.5, 1.75, 2.0, 0.0, 0.0},
                {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.5, 1.5, 1.0, 0.0, 0.0},
                {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.25, 1.25, 0.75, 0.0, 0.0},
                {0.0, 0.0, 0.0, 1.75, 1.5, 1.25, 1.0, 1.0, 0.75, 0.5, 0.0, 0.0},
                {0.0, 0.0, 0.0, 2.0, 3.75, 4.0, 0.75, 0.75, 1.0, 0.25, 0.0, 0.0},
                {0.0, 0.0, 0.0, 2.25, 3.5, 3.25, 0.5, 0.5, 1.0, 0.25, 0.0, 0.0},
                {0.0, 0.0, 0.0, 2.5, 2.75, 3.0, 0.25, 0.25, 0.0, 0.0, 0.0, 0.0},
                {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
                {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.1, 0.25, 0.25, 0.0},
                {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.25, 0.5, 0.25, 0.0},
                {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.25, 0.5, 0.25, 0.0},
                {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.25, 0.5, 0.25, 0.0}
        };


        float[][] map = new float[floatMap.length][floatMap[0].length];

        for (int i = 0; i < floatMap.length; i++) {
            for (int j = 0; j < floatMap[i].length; j++) {
                map[i][j] = (float) floatMap[i][j]; // Cast to float
            }
        }


        // Define the source (bottom-left) and destination (top-left).
        int[] src = {2, 2};
        int[] dest = {5, 4};
        List<List<Integer>> path_arr = new ArrayList<>(List.of());

        List<int[]> path = findPath(map, src, dest);
        if (path != null) {
            System.out.println("Path found:");
            for (int[] cell : path) {
                System.out.println("(" + cell[0] + ", " + cell[1] + ")");
                List<Integer> coordinates = new ArrayList<Integer>();
                coordinates.add(cell[0]);
                coordinates.add(cell[1]);
                System.out.println(coordinates);
                path_arr.add(coordinates);
            }
        } else {
            System.out.println("No path found.");
        }
        System.out.println("This is the path: " + path_arr);

        // Create the Swing frame for visualization.
        JFrame frame = new JFrame("A* Search Visualization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new AStarVisualizer(map, path));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
