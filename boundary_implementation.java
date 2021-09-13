import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

//$SIZE $finalPathID $GRAPH $MAX_ITERATIONS $NUM_CELLS
public class boundary_implementation {
    static boolean debug = true;
    static boolean[][] boundary;
    static int[][] grid; //maintain 0(outside), 1(cytoplasm), or 2 (boundary/cytoskeleton)
    static int gridSizeRow, gridSizeColumn, cellSize;
    static int[] dx = {1, 0, -1, 0};
    static boolean[][] shouldPrint;
    static int[] dy = {0, 1, 0, -1};
    static boolean connected;
    static int[][] color;
    static boolean[][] visited;
    static String path;
    static PrintWriter out;
    static int picMagnifier = 10;
    static int cellNum = 2;
    static long startTime;
    static int[] root; //implement disjoint set union to merge two cells when they merge
    static int[] size;
    static int[] containsAZ;
    static int numberOfCells = 0;
    static int[][] activeZone;
    static boolean[] discovered;
    static int[] map = new int[1000003]; //map
    static int trueCount = 0;
    static Random myRandom = new Random();
    static long maxNumIterations = 4000000;
    static double trueIterationCount = 0.0;
    static int trueArea = 0;

    static HashMap<Integer, Integer> twos_pos = new HashMap<>();// Stores the position of the key x*gridSize + y in the array twos
    static int[] twos; // Stores x*gridSize + y for the (x, y) where there is a 2

    public static void insert_two(int x, int y) {
        int num_twos = twos_pos.size();
        twos_pos.put(x*gridSizeColumn + y, num_twos);
        twos[num_twos] = x*gridSizeColumn + y;
    }

    public static void remove_two(int x, int y) {
        int num_twos = twos_pos.size();
        int position = twos_pos.get(x*gridSizeColumn + y);
        twos[position] = twos[num_twos-1];
        twos_pos.put(twos[num_twos-1], position);
        twos_pos.remove(x*gridSizeColumn + y);
    }

    public static int CELL( String file_name) throws IOException {

        visited = new boolean[gridSizeRow][gridSizeColumn];
        long numIterations = 1000000;
        int maxNumMoves = 1000;
        int maxBubbleNeighbors = 3;
        int lastSize = 0;
        int lastChange = 0;
        boolean done = false;
        int previousArea = allCytoplasm();
        while (true){
            //limit number of iterations
            if (trueCount > maxNumIterations){
                trueCount = -1;
                return -1;
            }
            trueCount++;

            //choose stimulus point
            int[] p = chooseStimulusPoint();
            int stim_x = p[0];
            int stim_y = p[1];

            //invade zero
            int dir = selectNext0(stim_x, stim_y);
            if (dir == -1) continue;
            change(stim_x, stim_y, 0);
            change(stim_x + dx[dir], stim_y + dy[dir], 2);

            color[stim_x + dx[dir]][stim_y + dy[dir]] = color[stim_x][stim_y];
            color[stim_x][stim_y] = 0;
            checkMerge(stim_x + dx[dir], stim_y + dy[dir]);

            reassignPoint(stim_x, stim_y);
            reassignPoint(stim_x + dx[dir], stim_y + dy[dir]);

            int bX = stim_x;
            int bY = stim_y;
            for (int numMoves = 0; numMoves < maxNumMoves || done; numMoves++) {
                visited[bX][bY] = true;
                int numBubbles = 0;
                for (int i = 0; i < 4; i++) {
                    if (!inBounds(bX + dx[i], bY + dy[i]) || grid[bX + dx[i]][bY + dy[i]] == 0) numBubbles++;
                }
                if (numBubbles >= maxBubbleNeighbors) break;

                dir = selectNext1or2(bX, bY);
                if (dir == -1) break;
//                System.out.println("bubble moves from "+bX+", "+bY+" to "+(bX + dx[dir])+", "+(bY + dy[dir]));

                change(bX, bY, 2);
                change(bX + dx[dir], bY + dy[dir], 0);
                reassignPoint(bX, bY);
                reassignPoint(bX + dx[dir], bY + dy[dir]);

                color[bX][bY] = color[bX + dx[dir]][bY + dy[dir]];
                color[bX + dx[dir]][bY + dy[dir]] = 0;
                bX += dx[dir];
                bY += dy[dir];
            }
            numberOfCells = getNumberOfCells();
            trueIterationCount += 1.0/numberOfCells;
            reset(visited);
            int numDiscovered = 0;
            for (boolean ii : discovered) if (ii) numDiscovered++;

            if (numDiscovered != discovered.length || numberOfCells != 1) continue;

            if (!done){
                lastChange = trueCount;
            }
            done = true;

            if (trueCount - lastChange > numIterations){
                System.out.println("breaking down below area is "+trueArea);
                System.out.println("counting distance is "+computeDistance());
                break;
            }
            if (trueArea != lastSize){
                lastSize = trueArea;
                lastChange = trueCount;
            }

        }
        return -1;

    }
    public static boolean inBounds(int x, int y){
        if (x < 0 || y < 0 || x >= grid.length || y >= grid[0].length) return false;
        return boundary[x][y];
    }
    //check if changing the value of (x, y) has connected two cells
    public static void checkMerge(int x, int y){
        for (int i = 0; i < 4; i++){
            if (inBounds(x + dx[i], y+ dy[i]) && getRoot(color[x + dx[i]][y + dy[i]]) != getRoot(color[x][y]) && color[x + dx[i]][y + dy[i]] != 0){
                merge(color[x + dx[i]][y + dy[i]], color[x][y]);
                numberOfCells--;
            }
        }
    }
    //update list of coordinates with value two
    public static void change(int x, int y, int after){
        int before = grid[x][y];
        if (before == after) return;
        if (before == 2) remove_two(x, y);
        if (after == 2) insert_two(x, y);
        grid[x][y] = after;
    }
    //change specific point, look at neighbors
    static void reassignSpecificPoint(int x, int y){
        if (grid[x][y] == 0) return;
        int numZeros = 0;
        for (int k = 0; k < 4; k++){
            if (!inBounds(x+dx[k], y+dy[k]) || grid[x + dx[k]][y + dy[k]] == 0) numZeros++;
        }
        if (numZeros == 0){
            change(x, y, 1);
//            grid[x][y] = 1;
        }
        else{
            change(x, y, 2);
//            grid[x][y] = 2;
        }
    }
    //if changing given x, y from 0 to 1 or 2 or vice versa, need to check if neighbors also change
    static void reassignPoint(int x, int y) {
        reassignSpecificPoint(x, y);
        for (int k = 0; k < 4; k++){
            if (inBounds(x + dx[k], y + dy[k])) reassignSpecificPoint(x + dx[k], y + dy[k]);
        }
    }
    public static void main(String[] args) throws Exception {
        long seed = myRandom.nextLong();
//        seed = -1145926789534253683l;
        System.out.println("seed "+seed);
        myRandom.setSeed(seed);
        startTime = System.currentTimeMillis();
        map[0] = Color.WHITE.getRGB();
        map[1] = Color.BLUE.getRGB();
        map[2] = Color.CYAN.getRGB();
        for (int i = 3; i < map.length; i++){
            int f = myRandom.nextInt(255 * 255);
            int r = f % 255;
            f = (f - r)/255;
            int g = f % 225;
            f = (f - g)/255;
            int b = f;
            map[i] = (new Color(r, g, b)).getRGB();
        }
        String pathID;
        String graphNumber;

        cellSize = 7;
        boolean square = true;
        int spacing = 1;

        graphNumber = "boundary_points";
        maxNumIterations = 1000000000000l;
        pathID = graphNumber;
        int trial = 1;
        path = "./images/two_cell/cytoplasm_trials_square/graph_"+graphNumber+"/size_"+cellSize+"/trial_"+trial+"/";

        while (new File(path).exists()){
            trial++;
            path = "./images/two_cell/cytoplasm_trials_square/graph_"+graphNumber+"/size_"+cellSize+"/trial_"+trial+"/";
        }
        System.out.println("graph_"+graphNumber+"/trial_"+trial);
        File file = new File(path);
        Files.createDirectories(file.toPath());


        FastScanner input = new FastScanner("grids/"+graphNumber+".txt");
//        if (M != 100){
//            numberOfCells = (M/(cellSize + 1)) * (M/(cellSize + 1));
//        }
        System.out.println("cellSize "+cellSize);
        input.next();

        int N = input.nextInt();
        System.out.println("cellSize "+cellSize+" pathId "+pathID+" graphNumber "+graphNumber+" numOfPoints "+N+" max iterations "+maxNumIterations);
        activeZone = new int[N][4];
        for (int i = 0; i < N; i++){
            for (int j = 0; j < 4; j++) activeZone[i][j] = input.nextInt();
        }
        gridSizeRow = 100;
        gridSizeColumn = 100;
        discovered = new boolean[activeZone.length];
        connected = false;
        grid = new int[gridSizeRow][gridSizeColumn];
        shouldPrint = new boolean[gridSizeRow][gridSizeColumn];
        color = new int[gridSizeRow][gridSizeColumn];
        boundary = new boolean[gridSizeRow][gridSizeColumn];
        String boundaryPath = "grids/boundary.csv";
        Scanner input2 = new Scanner(new BufferedReader(new FileReader(boundaryPath)));
        for (int i = 0; i < gridSizeRow; i++) {
            String f = input2.nextLine();
            String[] fDiv = f.split(",");
            for (int j = 0; j < gridSizeColumn; j++) {
                if (Integer.parseInt(fDiv[j]) == 1) {
                    boundary[i][j] = true;
                }
            }
        }
//        System.out.println(gridSize);
        if (debug) print(grid, "0");
//        System.exit(0);
        //spawn initial CELLs
        cellNum = 0;
        twos = new int[gridSizeRow*gridSizeColumn + 3];
        int numberCellsRow = (gridSizeRow)/(cellSize + spacing);
        double adderX = (gridSizeRow - numberCellsRow * cellSize)/(numberCellsRow + 0.0);

        int numberCellsCol = (gridSizeColumn)/(cellSize + spacing);
        double adderY = (gridSizeColumn - numberCellsCol * cellSize)/(numberCellsCol + 0.0);
        if (!square) {
            for (double x = adderX / 2 + cellSize / 2; x <= gridSizeRow - adderX / 2 - (cellSize - 1) / 2; x += cellSize + adderX) {
                for (double y = adderY / 2 + cellSize / 2; y <= gridSizeColumn - adderY / 2 - (cellSize - 1) / 2; y += adderY + cellSize) {
//                    System.out.println("x "+x+" y "+y);
                    spawnDiamond((int) Math.round(x), (int) Math.round(y), cellNum + 3);
                    cellNum++;
                }
            }
        }
        else {
            for (double x = adderX/2; Math.round(x) + cellSize <= gridSizeRow; x+= cellSize + adderX){
                for (double y = adderY/2; Math.round(y) + cellSize <= gridSizeColumn; y += adderY + cellSize){
                    //                    System.out.println();
                    spawnSquare((int) Math.round(x), (int) Math.round(y), cellNum + 3);
                    cellNum++;
                }
            }
        }
        String fileName = "size_"+cellSize+"_num_"+cellNum;

        if (debug) print(color, "00");
        root = new int[gridSizeRow * gridSizeColumn + 3];
        size = new int[gridSizeRow * gridSizeColumn + 3];
        containsAZ = new int[gridSizeRow * gridSizeColumn + 3];
        Arrays.fill(size, 1);
        for (int i = 0; i < root.length; i++){
            root[i] = i;
        }
        numberOfCells = cellNum;
        int ogCytoplasm = 0;
        for (int i = 0; i < grid.length; i++){
            for (int j = 0; j < grid[i].length; j++){
                if (grid[i][j] != 0) ogCytoplasm++;
            }
        }
        System.out.println("cytoplasm "+ogCytoplasm);
//        System.exit(0);

        //run algorithm
        CELL(fileName);
        //save statistics
        if (debug){
            System.out.println("size: "+cellSize);
            System.out.println("number of cells: "+cellNum);
            System.out.println("starting area: "+ogCytoplasm);
            System.out.println("iterations: "+trueCount);
            int finalDis = trueArea;
            if (trueCount == -1){
                finalDis = -1;
            }
            System.out.println("distance: "+finalDis);
            if (trueCount == -1) trueIterationCount = -1;
            System.out.println("true iteration count: " + trueIterationCount);
            System.out.println("time: "+(System.currentTimeMillis() - startTime));
        }
        else{
            out.println("size: "+cellSize);
            out.println("number of cells: "+cellNum);
            out.println("starting area: "+ogCytoplasm);
            out.println("iterations: "+trueCount);
            int finalDis = trueArea;
            if (trueCount == -1){
                finalDis = -1;
            }
            out.println("distance: "+finalDis);
            if (trueCount == -1) trueIterationCount = -1;
            out.println("true iteration count: " + trueIterationCount);
            out.println("time: "+(System.currentTimeMillis() - startTime));
            out.close();
        }

    }
    static int allCytoplasm(){
        int ret = 0;
        for (int i = 0; i < gridSizeRow; i++){
            for (int j = 0; j < gridSizeColumn; j++){
                if (grid[i][j] != 0){
                    ret ++;
                }
            }
        }
        return ret;
    }
    static boolean isInt(String f){
        try{
            Integer.parseInt(f);
            return true;
        }
        catch (Exception e){
            return false;
        }
    }
    //used to get baseline measurement for distance
    //uses BFS to only count cells which are actually connected to a point

    static int computeDistance(){
        int ret = 0;
        int need = 0;
        for (int[] i : activeZone){
            if (color[i[0]][i[2]] != 0){
                need = getRoot(color[i[0]][i[2]]);
            }
        }
        for (int i = 0; i < gridSizeRow; i++){
            for (int j = 0 ;j < gridSizeColumn; j++){
                if (getRoot(color[i][j]) == need){
                    ret++;
                }
                else{
                    change(i, j, 0);
                }
            }
        }
        return ret;
    }

    static void updateDiscovered(){
        for (int j = 0; j < activeZone.length; j++){
            int[] i = activeZone[j];
            boolean before = discovered[j];
            discovered[j] = false;
            outerloop:
            for (int x = i[0]; x <= i[1]; x++){
                for (int y = i[2]; y <= i[3]; y++){
                    if (grid[x][y] != 0){
                        discovered[j] = true;
                        break outerloop;
                    }
                }
            }
        }
    }

    //select adjacent square of value 0
    static int selectNext0(int bX, int bY){
        int dir[] = {0,1,2,3};
        for (int i=0; i<4; i++) {
            int idx = i + myRandom.nextInt(4-i);
            int xx = bX + dx[dir[idx]], yy = bY + dy[dir[idx]];
            if (inBounds(xx, yy) && grid[xx][yy] == 0 && !visited[xx][yy]) return dir[idx];
            dir[idx] = dir[i];
        }
        return -1;
    }
    //select adjacent square of value either 1 or 2
    static int selectNext1or2(int bX, int bY){
        int dir[] = {0,1,2,3};
        for (int i=0; i<4; i++) {
            int idx = i + myRandom.nextInt(4-i);
            int xx = bX + dx[dir[idx]], yy = bY + dy[dir[idx]];
            if (inBounds(xx, yy) && (grid[xx][yy] == 1 || grid[xx][yy] == 2) && !visited[xx][yy]) return dir[idx];
            dir[idx] = dir[i];
        }
        return -1;
    }
    //check if given coordinates are inside point
    static boolean inActiveZone(int x, int y){
        for (int[] i : activeZone){
            if (i[0] <= x && x <= i[1] && i[2] <= y && y <= i[3]){
                return true;
            }
        }
        return false;
    }
    //reset array to false
    static void reset(boolean[][] visit){
        for (int i = 0; i < visit.length; i++) Arrays.fill(visit[i], false);
    }
    //randomly choose any square of value 2
    static int[] chooseStimulusPointNoActive(){
        int index = (int) (myRandom.nextDouble() * twos_pos.size());
        return new int[] {twos[index]/gridSizeColumn, twos[index]%gridSizeColumn};
    }
    //randomly choose a square to remove from inside a point
    static void remove() throws IOException{
//        System.out.println("remove");
        if (trueArea == 0){
            trueArea = computeDistance();
            print(grid, trueCount+"_first_solve");
            if (debug){
                System.out.println("foraging stats");
                System.out.println("iterations: "+trueCount);
                System.out.println("true iteration count: " + trueIterationCount);
                System.out.println("time: "+(System.currentTimeMillis() - startTime));
            }
            else{
                out.println("foraging stats");
                out.println("iterations: "+trueCount);
                out.println("true iteration count: " + trueIterationCount);
                out.println("time: "+(System.currentTimeMillis() - startTime));
            }

        }
        trueArea--;
        int numPossible = 0;
        for (int j = 0; j < activeZone.length; j++){
            int[] i = activeZone[j];
            for (int x = i[0]; x <= i[1]; x++){
                for (int y = i[2]; y <= i[3]; y++){
                    if (grid[x][y] == 1) {
                        numPossible++;
                    }
                }
            }
        }
        int r = (int) (myRandom.nextDouble() * numPossible);
        int c = 0;
        outerloop:
        for (int j = 0; j < activeZone.length; j++){
            int[] i = activeZone[j];
            for (int x = i[0]; x <= i[1]; x++){
                for (int y = i[2]; y <= i[3]; y++){
                    if (grid[x][y] != 1) continue;
                    if (c == r){
                        change(x, y, 0);
                        reassignPoint(x, y);
                        break outerloop;
                    }
                    c++;
                }
            }
        }
    }

    //randomly choose a square marked 2 that is inside a point
    static int[] chooseStimulusPointActive() throws IOException{
//        System.out.println("active");
        int numPossible = 0;
//        System.out.println(Arrays.deepToString(activeZone));
        for (int j = 0; j < activeZone.length; j++){
            int[] i = activeZone[j];
            for (int x = i[0]; x <= i[1]; x++){
                for (int y = i[2]; y <= i[3]; y++){
                    if (grid[x][y] == 2) {
                        numPossible++;
                    }
                }
            }
        }
//        System.out.println(numPossible);
        int r = (int) (myRandom.nextDouble() * numPossible);
//        System.out.println("numPossible "+numPossible+" r " + r);
        int c = 0;
        for (int j = 0; j < activeZone.length; j++){
            int[] i = activeZone[j];
            for (int x = i[0]; x <= i[1]; x++){
                for (int y = i[2]; y <= i[3]; y++){
                    if (grid[x][y] != 2) continue;
                    if (c == r){
                        return new int[] {x, y};
                    }
                    c++;
                }
            }
        }
        return new int[] {-1, -1};
    }
    static int[] chooseStimulusPoint() throws IOException{
        updateDiscovered();
        int numDiscovered = 0;
        int numCells = getNumberOfCells();
        for (boolean i : discovered) if (i) numDiscovered++;
        int numNotDiscovered = discovered.length - numDiscovered;
        double nonActiveProb = (numNotDiscovered + numCells - 1)/((double) discovered.length + cellNum);

        if (trueCount % 100000 == 0 && debug){
            System.out.println("iteration="+trueCount+"; numNotDiscovered="+numNotDiscovered+"; numCells="+numCells+"; cellNum="+cellNum+"; p = "+nonActiveProb+"; area="+trueArea);
            print(grid, ""+trueCount);
        }
        if (trueCount % 1000000 == 0 && !debug){
            System.out.println("iterations "+trueCount+" numNotDiscovered "+numNotDiscovered+" numCells "+numCells);
        }

        //try to choose according to probability
        double r = myRandom.nextDouble();
        if (r < nonActiveProb) return  chooseStimulusPointNoActive();
        int[] f =  chooseStimulusPointActive();
        //if chooseStimulusPointFails because there are no 2s inside your points, just choose stimulusPointNoActive()
        if (f[0] != -1) return f;
        if (nonActiveProb != 0 || numCells != 1){
            return chooseStimulusPointNoActive();
        }
        //if all active points are found, like when shrinking,
        // we remove cytoplasm from inside one of the points,
        // creating some 2s, and then choose those as the stimulus point
        remove();
//        System.out.println("removing");
        return chooseStimulusPointActive();
    }
    //spawn a CELL of cellSize at (spawnX, spawnY) with a given id
    static int spawnSquare(int spawnX, int spawnY, int id) throws IOException{
        for (int i = spawnX; i < cellSize + spawnX; i++){
            for (int j = spawnY; j < cellSize + spawnY; j++){
                if (!boundary[i][j]) continue;
                change(i, j, 1);
                color[i][j] = id;
            }
        }
        for (int i = spawnX; i < cellSize + spawnX; i++){
            if (boundary[i][spawnY]) change(i, spawnY, 2);
            if (boundary[i][spawnY + cellSize - 1]) change(i, spawnY + cellSize - 1, 2);
        }
        for (int j = spawnY; j < cellSize + spawnY; j++){
            if (boundary[spawnX][j]) change(spawnX, j, 2);
            if (boundary[spawnX + cellSize - 1][j]) change(spawnX + cellSize - 1, j, 2);
        }
        return 0;
    }
    static int spawnDiamond(int spawnX, int spawnY, int id) throws IOException{
        for (int i = 0; i <= cellSize/2; i++){
            if (boundary[spawnX+i][-cellSize/2 + i +spawnY]){
                change(spawnX + i, -cellSize / 2 + i + spawnY, 2);
                color[spawnX + i][-cellSize/2 + i + spawnY] =id;

            }
            if (boundary[spawnX+i][cellSize/2 - i +spawnY]){
                change(spawnX + i, cellSize/2 - i + spawnY, 2);
                color[spawnX + i][cellSize/2 - i + spawnY] = id;
            }
            if (boundary[spawnX-i][-cellSize/2 + i +spawnY]){
                change(spawnX - i, -cellSize/2 + i + spawnY, 2);
                color[spawnX - i][-cellSize/2 + i + spawnY] =id;

            }
            if (boundary[spawnX-i][cellSize/2 - i +spawnY]){
                change(spawnX - i, cellSize/2 - i + spawnY, 2);
                color[spawnX - i][cellSize/2 - i + spawnY] = id;

            }
            for (int j = -cellSize/2 + i + spawnY + 1; j <= cellSize/2 - i + spawnY - 1; j++) {
                if (boundary[spawnX+i][j]) {
                    change(spawnX + i, j, 1);
                    color[spawnX + i][j] = id;
                }
                if (boundary[spawnX-i][j]) {
                    change(spawnX - i, j, 1);
                    color[spawnX - i][j] = id;
                }
            }
        }
        return 0;
    }


    //disjoint set union methods
    static int getRoot(int index){
        if (root[index] == index) return index;
        int r = getRoot(root[index]);
        root[index] = r;
        return r;
    }
    static void merge(int i1, int i2){
        int r1 = getRoot(i1);
        int r2 = getRoot(i2);
        if (size[r2] > size[r1]){
            int a = r2;
            r2 = r1;
            r1 = a;
        }
        root[r2] = r1;
        size[r1] +=  size[r2];
    }
    static int getNumberOfCells(){
        int[] seen = new int[root.length];
        int num = 0;
        for (int[] i : activeZone){
            for (int x = i[0]; x <= i[1]; x++){
                for (int y = i[2]; y <= i[3]; y++){
                    if (color[x][y] != 0){
                        int r = getRoot(color[x][y]);
                        if (seen[r] == 0){
                            seen[r] = 1;
                            num++;
                        }
                    }
                }
            }
        }
        return num;
    }
    //code to handle image printing
    static void print(int[][] arr, String iteration) throws IOException {
        int height = picMagnifier * arr.length;
        int width = picMagnifier * arr[0].length;
        int[] pixels = new int[width*height];
        for (int j = 0; j < arr[0].length; j++) {
            for (int i = 0; i < arr.length; i++) {
//                System.out.println(arr[i][j]);
//                if (debug & arr[i][j] == -1){
//                    color(pixels, i, j, Color.yellow.getRGB());
//                }

                color(pixels, i, j, map[arr[i][j]]);
                if (boundary[i][j] && arr[i][j] == 0){
                    color(pixels, i, j, Color.LIGHT_GRAY.getRGB());
                }

            }
        }

        for (int[] i : activeZone){
            for (int x = i[0]; x <= i[1]; x++){
                for (int y = i[2]; y <= i[3]; y++){
                    color(pixels, x, y, Color.GRAY.getRGB());
                }
            }
        }
        for (int[] a : activeZone){

            for (int i = a[0] * picMagnifier + 3; i < (a[1] + 1) * picMagnifier - 3; i++){
                for (int j = a[2] * picMagnifier + 3; j < (a[3] + 1) * picMagnifier - 3; j++){

                    //                System.out.println("nX "+nX+" nY "+nY+" pixels index " + (nY * grid[0].length + nX));
                    //                System.out.println(pixels.length);
                    //                System.out.println(Arrays.toString(pixels));
                    if (arr[i/picMagnifier][j/picMagnifier] == -1 && debug){
                        pixels[j* picMagnifier * grid[0].length + i] = Color.yellow.getRGB();
                    }
                    else pixels[i* picMagnifier * grid[0].length + j] = map[arr[i/picMagnifier][j/picMagnifier]];
                }
            }

        }

        String fileName = path +"pic_"+ iteration+".png";
        BufferedImage pixelImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        pixelImage.setRGB(0, 0, width, height, pixels, 0, width);
        File outputfile = new File(fileName);
        ImageIO.write(pixelImage, "png", outputfile);
    }
    static void color(int[] pixels, int x, int y, int color){
        for (int i = 0; i < picMagnifier; i++){
            for (int j = 0; j < picMagnifier; j++){

                int nX = picMagnifier * x + i;
                int nY = picMagnifier * y + j;
                pixels[nX * picMagnifier* grid[0].length + nY] = color;
            }
        }
    }
    //static void color(int[] pixels, int x, int y, int color){
//    for (int i = 0; i < picMagnifier; i++){
//        for (int j = 0; j < picMagnifier; j++){
//            int nX = picMagnifier * x + i;
//            int nY = picMagnifier * y + j;
//            pixels[nY * picMagnifier* grid[0].length + nX] = color;
//        }
//    }
//}
    //IO class
    static class FastScanner {
        BufferedReader br;
        StringTokenizer st;

        public FastScanner(InputStream stream) {
            br = new BufferedReader(new InputStreamReader(stream));
            st = new StringTokenizer("");
        }

        public FastScanner(String fileName) throws Exception {
            br = new BufferedReader(new FileReader(new File(fileName)));
            st = new StringTokenizer("");
        }

        public String next() throws Exception {
            while (!st.hasMoreTokens()) {
                st = new StringTokenizer(br.readLine());
            }
            return st.nextToken();
        }

        public int nextInt() throws Exception {
            return Integer.parseInt(next());
        }

        public long nextLong() throws Exception {
            return Long.parseLong(next());
        }

        public Double nextDouble() throws Exception {
            return Double.parseDouble(next());
        }

        public String nextLine() throws Exception {
            if (st.hasMoreTokens()) {
                StringBuilder str = new StringBuilder();
                boolean first = true;
                while (st.hasMoreTokens()) {
                    if (first) {
                        first = false;
                    } else {
                        str.append(" ");
                    }
                    str.append(st.nextToken());
                }
                return str.toString();
            } else {
                return br.readLine();
            }
        }
    }
}
