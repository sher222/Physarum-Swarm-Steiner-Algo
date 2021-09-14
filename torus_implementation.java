import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class torus_implementation {
    //static int num = 1;
    static boolean debug = true;
    static boolean choseActive = false;
    static int[][] grid;
    static int counter = 0;
    static int gridSize, cellSize;
    static int M;
    static int[] dx = {1, 0, -1, 0};
    static int[] dy = {0, 1, 0, -1};
    static boolean connected;
    static int[][] color;
    static boolean[][] visited;
    static String path;
    static PrintWriter out;
    static int picMagnifier = 10;
    static int directoryNum = 1;
    static int cellNum = 2;
    static int numIterationsStuck = 0;
    static long startTime;
    static double prevX, prevY;
    static int[] root;
    static int[] size;
    static int numberOfCells = 0;
    //    static int[][] activeZone = {{52, 53, 53, 54}, {46, 47, 59, 60}, {59, 60, 49, 50}, {47, 48, 42, 43}, {50, 51, 41, 42}, {55, 56, 43, 44}, {43, 44, 49, 50}};
    static int[][] activeZone;
    static boolean[] discovered;
    static int[] map = new int[1000003];
    static int trueCount = 0;
    static HashMap<String, Integer> twos = new HashMap<>();
    static Random myRandom = new Random();
    static int maxNumIterations = 4000000;
    static double trueIterationCount = 0.0;
    static int trueArea = 0;
//    static long startTimeVar;
    public static int CELL( String file_name) throws IOException {
//        long timing = System.currentTimeMillis();
//        updateDiscovered();
        visited = new boolean[gridSize][gridSize];
//        print(grid, file_name+"_0");

        int numIterations = 1000000;
        int maxNumMoves = 1000;
        int maxBubbleNeighbors = 3;
        int lastSize = 0;
        int lastChange = 0;
        boolean done = false;
        while (true){
            if (!done && trueCount > maxNumIterations){
//                System.out.println("time "+(System.currentTimeMillis() - startTimeVar)/1000);
                trueCount = -1;
                System.out.println("set trueCount = -1");
                return -1;
            }
            trueCount++;
//            if (trueCount > 20000 && trueCount % 10 == 0){
//                print(grid, file_name + "_" + trueCount);
//            }
//            if (trueCount > 10) break;
//            if (trueCount % 1000 == 0){
//                print(color, file_name + "_" + trueCount);
////                System.out.println("time "+(System.currentTimeMillis() - timing));
////                timing = System.currentTimeMillis();
//            }
//            if (connected) {
//                break;
//            }
//            System.out.println(loop);
            //choose stimulus point
            int[] p = chooseStimulusPoint();
            //System.out.println(Arrays.toString(p));
            int stim_x = p[0];
            int stim_y = p[1];

            //invade zero
            int dir = selectNext0(stim_x, stim_y);
            if (dir == -1) continue;
            int f = grid[mod(stim_x + dx[dir])][mod(stim_y +  dy[dir])];
            grid[mod(stim_x + dx[dir])][mod(stim_y + dy[dir])] = -1;
//            if (debug && choseActive && trueCount > 400000) {
//                print(grid, trueCount + "_" + "0" + "_grid");
//            }
            grid[mod(stim_x + dx[dir])][mod(stim_y + dy[dir])] = f;
            reassignPoint(stim_x, stim_y);
            change(mod(stim_x + dx[dir]), mod(stim_y + dy[dir]), 2);
//            grid[stim_x + dx[dir]][stim_y + dy[dir]] = 2;
            reassignPoint(mod(stim_x + dx[dir]), mod(stim_y + dy[dir]));
            color[mod(stim_x + dx[dir])][mod(stim_y + dy[dir])] = color[stim_x][stim_y];
            checkMerge(mod(stim_x + dx[dir]), mod(stim_y + dy[dir]));

            change(stim_x, stim_y, 0);
//            grid[stim_x][stim_y] = 0;
            color[stim_x][stim_y] = 0;
            f = grid[stim_x][stim_y];
            grid[stim_x][stim_y] = -1;
//            if (debug && choseActive && trueCount > 400000) {
//                print(grid, trueCount + "_" + "1" + "_grid");
//            }
            grid[stim_x][stim_y] = f;
//            if (debug && choseActive && trueCount > 400000) {
//                print(grid, trueCount + "_" + "1" + "_grid");
//            }
//            print(grid);
            //replace all 1s with 2s
//            for (int i = 0; i < gridSize; i++) {
//                for (int j = 0; j < gridSize; j++) {
//                    if (grid[i][j] == 1) {
//                        grid[i][j] = 2;
//                    }
//                }
//            }
//            print(grid);
            int bX = stim_x;
            int bY = stim_y;
            for (int numMoves = 0; numMoves < maxNumMoves; numMoves++) {
                visited[bX][bY] = true;
                int numBubbles = 0;
                for (int i = 0; i < 4; i++) {

                    if (grid[mod(bX + dx[i])][mod(bY + dy[i])] == 0) numBubbles++;
                }
                if (numBubbles >= maxBubbleNeighbors) break;

                dir = selectNext1or2(bX, bY);
                if (dir == -1) break;
//                System.out.println("bubble moves from "+bX+", "+bY+" to "+(bX + dx[dir])+", "+(bY + dy[dir]));
                color[bX][bY] = color[mod(bX + dx[dir])][mod(bY + dy[dir])];
                change(bX, bY, 2);
//                grid[bX][bY] = 2;
                change(mod(bX + dx[dir]), mod(bY + dy[dir]), 0);
//                grid[bX + dx[dir]][bY + dy[dir]] = 0;
                color[mod(bX + dx[dir])][mod(bY + dy[dir])] = 0;
                reassignPoint(bX, bY);
                reassignPoint(mod(bX + dx[dir]), mod(bY + dy[dir]));
//                if (debug && choseActive && trueCount > 400000) {
//                    color[bX + dx[dir]][bY + dy[dir]] = -1;
//                    int og = grid[bX + dx[dir]][bY + dy[dir]];
//                    grid[bX + dx[dir]][bY + dy[dir]] = 3;
//                    System.out.println("trueCount "+trueCount+" numMoves "+numMoves);
////                    print(color, trueCount+"_"+numMoves+"_color");
//                    print(grid, trueCount+"_"+(numMoves + 2)+"_grid");
//                    grid[bX + dx[dir]][bY + dy[dir]] = og;
//                    color[bX + dx[dir]][bY + dy[dir]] = 0;
//                }
                bX = mod(bX + dx[dir]);
                bY = mod(bY + dy[dir]);
//                System.out.println("numMoves "+numMoves);
//                print(grid);
            }
            //reassign grid
//            reassign();
            trueIterationCount += 1.0/numberOfCells;
            reset(visited);
            int numDiscovered = 0;
            for (boolean ii : discovered) if (ii) numDiscovered++;

//            if ((System.currentTimeMillis() - startTime) > 3600000){
//                trueCount = -1;
//                System.out.println("timeout, -1");
//                break;
//            }
            if (numDiscovered != discovered.length || numberOfCells != 1) continue;
            //          System.out.println("dis "+dis+" dif "+(trueCount-lastChange));
//            System.out.println("done");
            //first time
            if (!done){
                lastChange = trueCount;
            }
            done = true;

            if (trueCount - lastChange > numIterations){
                System.out.println("breaking down below area is "+trueArea);
                break;
            }
            if (trueArea != lastSize){
                lastSize = trueArea;
                lastChange = trueCount;
            }



//            System.out.println("trueCount "+trueCount+" loop "+loop);
        }
//        print(grid, file_name+"_final");

        return -1;
    }
    public static boolean inBounds(int x, int y){
        if (x < 0 || y < 0 || x >= grid.length || y >= grid[0].length) return false;
        return true;
    }
    public static void checkMerge(int x, int y){
        for (int i = 0; i < 4; i++){
            int nX = mod(x + dx[i]);
            int nY = mod(y + dy[i]);
            if (getRoot(color[nX][nY]) != getRoot(color[x][y]) && color[nX][nY] != 0){
//                System.out.println("subtracting numbner of cells");
                merge(color[nX][nY], color[x][y]);
                numberOfCells--;
//                System.out.println(numberOfCells);
            }
        }
    }
    public static void change(int x, int y, int after){
        int before = grid[x][y];
        if (before == after) return;
        if (before == 2){
            twos.remove(x+" "+y);
        }
        if (after == 2){
            twos.put(x+" "+y, 1);
        }
        grid[x][y] = after;
    }
    static void reassignSpecificPoint(int x, int y){
        if (grid[x][y] == 0) return;
        int numZeros = 0;

        for (int k = 0; k < 4; k++){
            int nX = mod(x + dx[k]);
            int nY = mod(y + dy[k]);
            if (nX < 0 || nY < 0 || nX >= grid.length || nY >= grid[0].length){
                numZeros++;
                continue;
            }
            if (grid[nX][nY] == 0) numZeros++;
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
    static void reassignPoint(int x, int y) {
        reassignSpecificPoint(x, y);
        for (int k = 0; k < 4; k++){
            int nX = mod(x + dx[k]);
            int nY = mod(y + dy[k]);
            reassignSpecificPoint(nX, nY);
        }
    }
//    static ArrayList<int[]> queue = new ArrayList<>(40000);
//    static int checkConnected(){
//        int[][] ID = new int[grid.length][grid[0].length];
//        boolean[][] vis = new boolean[grid.length][grid[0].length];
//        int index = 0;
//
//        int notActive = 0;
//        int[] startingPoint = {0, 0};
////        System.out.println("here we go");
//        while (true){
//            int j = startingPoint[1];
//            outerloop:
//            for (int i = startingPoint[0]; i < ID.length; i++){
//                for (; j < ID.length; j++){
////                    System.out.println("i "+i+" j "+j);
//                    if (grid[i][j] == 0) continue;
//                    if (ID[i][j] == 0){
//                        index++;
//                        ID[i][j] = index;
//                        queue.add(new int[] {i, j});
//                        startingPoint[0] = i;
//                        startingPoint[1] = j;
//                        break outerloop;
//                    }
//                }
//                j = 0;
//            }
//            if (queue.isEmpty()) break;
//            boolean active = false;
//            while (!queue.isEmpty()){
//                int[] c = queue.remove(0);
////                if (vis[c[0]][c[1]]) continue;
//                vis[c[0]][c[1]] = true;
//                if (inActiveZone(c[0], c[1])){
//                    active = true;
//                }
//                ID[c[0]][c[1]] = index;
//                for (int k = 0; k < 4; k++){
//                    if (c[0] + dx[k] < 0 || c[0] + dx[k] >= vis.length || c[1] + dy[k] < 0 || c[1] + dy[k] >= vis[0].length) continue;
//                    if (vis[c[0] + dx[k]][c[1]+dy[k]] || grid[c[0] + dx[k]][c[1] + dy[k]] == 0) continue;
//                    vis[c[0] + dx[k]][c[1] + dy[k]] = true;
//                    queue.add(new int[] {c[0] + dx[k], c[1] + dy[k]});
//                }
//            }
//            if (!active) notActive++;
//        }
//        return index - notActive;
//    }
    public static void main(String[] args) throws Exception {
//       FastScanner input = new FastScanner("standard_implementation.in");
        long seed = myRandom.nextLong();
//        long seed = -6437055770315070887l;
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
        if (!debug) {
            cellSize = Integer.parseInt(args[0]);
//            String t = "0"+args[1];
//            pathID = t.substring(t.length() - 2);
            pathID=args[1];
            graphNumber = args[2];
            maxNumIterations = Integer.parseInt(args[3]);
        }
        else {
            cellSize = 9;
            graphNumber = "torus2";
            maxNumIterations = 1000000000;
            pathID = graphNumber;
            int trial = 1;
            path = "./images/two_cell/graph_"+graphNumber+"/trial_"+trial+"/";

            while (new File(path).exists()){
                trial++;
                path = "./images/two_cell/graph_"+graphNumber+"/trial_"+trial+"/";
            }
            System.out.println("graph_"+graphNumber+"/trial_"+trial);
            File file = new File(path);
            Files.createDirectories(file.toPath());

        }
        //start at 19 for 1 cell
        //start at 13 for 2 cells



//        String currentDir = System.getProperty("user.dir");
//        System.out.println("Current dir using System:" + currentDir);
        FastScanner input = new FastScanner(graphNumber+".txt");
        M = input.nextInt();
//        if (Integer.parseInt(graphNumber) >= 100){
//            M = input.nextInt();
//        }
        String fileName = "size_"+cellSize+"_num_"+cellNum;
        System.out.println("cellSize "+cellSize);
        int N = input.nextInt();
        System.out.println("M "+M+" N "+N);
        System.out.println("cellSize "+cellSize+" pathId "+pathID+" graphNumber "+graphNumber+" numOfPoints "+N+" max iterations "+maxNumIterations);

        activeZone = new int[N][4];
        for (int i = 0; i < N; i++){
            for (int j = 0; j < 4; j++) activeZone[i][j] = input.nextInt();
        }
        gridSize = M;
        discovered = new boolean[activeZone.length];
        connected = false;
        grid = new int[gridSize][gridSize];
        color = new int[gridSize][gridSize];

//        spawn(cellSize/2, gridSize/2, 2);
//        spawn(gridSize - cellSize/2 - 1, gridSize/2, 3);
       if (debug) print(grid, "0");

        cellNum = 0;
        int numCellsThatWillFit = (gridSize/(cellSize + 1));
//        System.out.println(numCellsThatWillFit);
        double adder = (gridSize - numCellsThatWillFit * cellSize)/(numCellsThatWillFit + 0.0);
//        int edgesOverflow = (gridSize - numCellsThatWillFit * (cellSize + 1))/2;
//        System.out.println("adder "+adder);
//        System.out.println("min "+(adder + cellSize/2.0));
//        System.out.println("max "+(gridSize - 1 - adder - cellSize/2.0));
        for (double x = adder/2 + cellSize/2; x <= gridSize - adder/2 - cellSize/2; x+= cellSize + adder){
            for (double y = adder/2 + cellSize/2; y <= gridSize  - adder/2 - cellSize/2; y += adder + cellSize){
//                System.out.println(Math.round(x)+", "+Math.round(y));
//                System.out.println("x "+x+" y "+y);
                spawn((int) Math.round(x), (int) Math.round(y), cellNum + 3);
                cellNum++;
            }
        }
       if (debug) print(grid, "00");
        root = new int[M *M  + 3];
        size = new int[M * M + 3];
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
//        Long timetime = System.currentTimeMillis();
        CELL(fileName);
//        System.out.println("time "+(System.currentTimeMillis() - startTime)/1000);
        //  print(grid, fileName+"_end");
        if (debug){
            System.out.println("size: "+cellSize);
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
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("/mount/efs/"+(pathID)+".txt")));
            out.println("size: "+cellSize);
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
    static int computeDistance(){
        int ret = 0;
        int[][] ID = new int[grid.length][grid[0].length];
        boolean[][] vis = new boolean[grid.length][grid[0].length];
        int index = 0;
        LinkedList<int[]> queue = new LinkedList<>();
        while (true){
            outerloop:
            for (int i = 0; i < ID.length; i++){
                for (int j = 0; j < ID.length; j++){
                    if (grid[i][j] == 0) continue;
                    if (ID[i][j] == 0){
                        index++;
                        ID[i][j] = index;
                        queue.add(new int[] {i, j});
                        break outerloop;
                    }
                }
            }
            if (queue.isEmpty()) break;
            boolean active = false;
            int size = 0;
            while (!queue.isEmpty()){
                int[] c = queue.poll();
                if (vis[c[0]][c[1]]) continue;
                vis[c[0]][c[1]] = true;
                if (inActiveZone(c[0], c[1])){
                    active = true;
                }
                ID[c[0]][c[1]] = index;
                size++;
                for (int k = 0; k < 4; k++){
                    int nX = mod(c[0] + dx[k]);
                    int nY = mod(c[1] + dy[k]);
                    if (vis[nX][nY] || grid[nX][nY] == 0) continue;
                    queue.add(new int[] {nX, nY});
                }
            }
            if (active) ret += size;
        }
        return ret;
    }
    static void updateDiscovered(){
//        Arrays.fill(discovered, false);

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
            if (before != discovered[j]) numIterationsStuck = -1;
        }
        numIterationsStuck++;
    }
    static void print(int[][] arr, String iteration) throws IOException {
//        for (int[] i : arr){
//            String out = "";
//            for (int j : i){
//                out += j + " ";
//            }
//            System.out.println(out.substring(0, out.length() - 1));
//        }
//        System.out.println("printing");
        int height = picMagnifier * arr.length;
        int width = picMagnifier * arr[0].length;
        int[] pixels = new int[width*height];
//        System.out.println();
//        for (int[] i : arr) System.out.println(Arrays.toString(i));

        for (int j = 0; j < arr.length; j++) {
            for (int i = 0; i < arr[0].length; i++) {
//                System.out.println(arr[i][j]);
                if (debug & arr[i][j] == -1){
                    color(pixels, i, j, Color.yellow.getRGB());
                }
                else color(pixels, i, j, map[arr[i][j]]);
//                if (arr[i][j] == 0){
//                    color(pixels,i, j, Color.WHITE.getRGB());
//                }
//                if (arr[i][j] == 1){
//                    color(pixels,i, j, Color.GRAY.getRGB());
//                }
//                if (arr[i][j] == 2){
//                    color(pixels,i, j, Color.BLACK.getRGB());
//                }

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
                    else pixels[j* picMagnifier * grid[0].length + i] = map[arr[i/picMagnifier][j/picMagnifier]];
                }
            }

        }

        String fileName = path +"pic_"+ iteration+".png";
//        System.out.println("printing at "+fileName);
//        System.out.println(Arrays.toString(pixels));
//        System.out.println("pixels length "+pixels.length);
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
//                System.out.println("nX "+nX+" nY "+nY+" pixels index " + (nY * grid[0].length + nX));
//                System.out.println(pixels.length);
//                System.out.println(Arrays.toString(pixels));
                pixels[nY * picMagnifier* grid[0].length + nX] = color;
            }
        }
    }
    static int mod(int v){
        return (v + M) % M;
    }
    static int selectNext0(int bX, int bY){
        int dir = (int) (myRandom.nextDouble() * 4);
        boolean[] tried = new boolean[4];
        while (grid[mod(bX + dx[dir])][mod(bY + dy[dir])] != 0 || visited[mod(bX + dx[dir])][mod(bY + dy[dir])]){
//            System.out.println("stuck in loop maybe");
            tried[dir] = true;
            if (allTrue(tried)){
                //System.out.println("returning -1");
                return -1;
            }
            dir = (int) (myRandom.nextDouble() * 4);
        }
//        System.out.println((bX + dx[dir])+", "+(bY + dy[dir]));
        return dir;
    }
    static int selectNext1or2(int bX, int bY){
        int dir = (int) (myRandom.nextDouble() * 4);
        boolean[] tried = new boolean[4];
        while ((grid[mod(bX + dx[dir])][mod(bY + dy[dir])] != 1 &&  grid[mod(bX + dx[dir])][mod(bY + dy[dir])] != 2)|| visited[mod(bX + dx[dir])][mod(bY + dy[dir])]){
//            System.out.println("stuck in loop maybe");
            tried[dir] = true;
            if (allTrue(tried)){
                //System.out.println("returning -1");
                return -1;
            }
            dir = (int) (myRandom.nextDouble() * 4);
        }
//        System.out.println((bX + dx[dir])+", "+(bY + dy[dir]));
        return dir;
    }
    static boolean inActiveZone(int x, int y){
        for (int[] i : activeZone){
//            System.out.println(Arrays.toString(i)+", "+x+", "+y);
            if (i[0] <= x && x <= i[1] && i[2] <= y && y <= i[3]){
//                System.out.println("x "+x+" y "+y+" verdict "+"true");

                return true;
            }
        }
//        System.out.println("x "+x+" y "+y+" verdict "+"false");
        return false;
    }
    static void reset(boolean[][] visit){
        for (int i = 0; i < visit.length; i++) Arrays.fill(visit[i], false);
    }
    static int[] chooseStimulusPointNoActive(){
//        System.out.println("noActive");
        int numPossible = twos.size();
//        System.out.println("trueCount "+trueCount+" numPossible "+numPossible);
//        for (int i = 0; i < gridSize; i++){
//            for (int j = 0; j < gridSize; j++){
////                System.out.println("i "+i+" j "+j);
//                if (grid[i][j] == 2) numPossible++;
//            }
//        }
//        System.out.println(numPossible);
        int r = (int) (myRandom.nextDouble() * numPossible);
        int c = 0;
        for (String i : twos.keySet()){

            if (c == r){
//                System.out.println(i);
                return new int[] {Integer.parseInt(i.split(" ")[0]), Integer.parseInt(i.split(" ")[1])};
            }
            c++;


        }
//        System.out.println("returning -1");

        return new int[] {-1, -1};
    }
    static void remove(){
//        System.out.println("remove");
        if (trueArea == 0) trueArea = computeDistance();
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
//        System.out.println("R "+r+" C "+c+" numPossible "+numPossible);
        outerloop:
        for (int j = 0; j < activeZone.length; j++){
            int[] i = activeZone[j];
            for (int x = i[0]; x <= i[1]; x++){
                for (int y = i[2]; y <= i[3]; y++){
                    if (grid[x][y] != 1) continue;
                    if (c == r){
//                        System.out.println("set "+x+", "+y+" to zero");
//                        grid[x][y] = 0;
                        change(x, y, 0);
                        reassignPoint(x, y);
                        break outerloop;
                    }
                    c++;
                }
            }
        }
//        System.out.println("reassign");
//        reassign();
    }

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
//                        System.out.println("found stimulus point");
                        choseActive = true;
                        return new int[] {x, y};
                    }
                    c++;
                }
            }
        }
        return new int[] {-1, -1};

    }
    static int[] chooseStimulusPoint() throws IOException{
        choseActive = false;
//        System.out.println("choose stimulus point");
        updateDiscovered();
        int numDiscovered = 0;
        int numCells = numberOfCells;
        for (boolean i : discovered) if (i) numDiscovered++;
//        System.out.println(Arrays.toString(discovered));
        int numNotDiscovered = discovered.length - numDiscovered;
        if (trueCount % 100000 == 0 && debug){
            System.out.println("iterations "+trueCount+" numNotDiscovered "+numNotDiscovered+" numCells "+numCells);
            print(grid, ""+trueCount);
        }
        if (trueCount % 1000000 == 0 && !debug){
            System.out.println("iterations "+trueCount+" numNotDiscovered "+numNotDiscovered+" numCells "+numCells);
        }
        double nonActiveProb = (numNotDiscovered + numCells - 1)/((double) discovered.length + cellNum);

//        nonActiveProb *= (1+ numIterationsStuck/10000.0);
//        System.out.println(nonActiveProb);
        double r = myRandom.nextDouble();
//        System.out.println(checkConnected());
//        System.out.println("probability of choosing noActive "+nonActiveProb);
        if (r < nonActiveProb) return  chooseStimulusPointNoActive();
        int[] f =  chooseStimulusPointActive();
//        System.out.println(Arrays.toString(f));
        if (f[0] != -1) return f;
//        System.out.println("F was -1");
        if (nonActiveProb != 0 || numCells != 1){
//            System.out.println(nonActiveProb);
            return chooseStimulusPointNoActive();
        }
//        System.out.println(checkConnected());
//        System.out.println("removing");
        remove();
        return chooseStimulusPointActive();
    }
    static int spawnCount = 0;

    static int spawn(int spawnX, int spawnY, int id) throws IOException{
//        System.out.println("spawnX "+spawnX+" spawnY "+spawnY);

        for (int i = 0; i <= cellSize/2; i++){
            change(spawnX + i, -cellSize/2 + i + spawnY, 2);
            change(spawnX + i, cellSize/2 - i + spawnY, 2);
            change(spawnX - i, -cellSize/2 + i + spawnY, 2);
            change(spawnX - i, cellSize/2 - i + spawnY, 2);
//            grid[spawnX + i][-cellSize/2 + i + spawnY] = 2;
//            grid[spawnX + i][cellSize/2 - i + spawnY] = 2;
//            grid[spawnX - i][-cellSize/2 + i + spawnY] = 2;
//            grid[spawnX - i][cellSize/2 - i + spawnY] = 2;
            color[spawnX + i][-cellSize/2 + i + spawnY] =id;
            color[spawnX + i][cellSize/2 - i + spawnY] = id;
            color[spawnX - i][-cellSize/2 + i + spawnY] =id;
            color[spawnX - i][cellSize/2 - i + spawnY] = id;
            for (int j = -cellSize/2 + i + spawnY + 1; j <= cellSize/2 - i + spawnY - 1; j++) {

                change(spawnX + i, j, 1);
                change(spawnX - i, j, 1);

//                grid[spawnX + i][j] = 1;
//                grid[spawnX - i][j] = 1;
                color[spawnX + i][j] = id;
                color[spawnX - i][j] = id;
            }
        }
//        System.out.println(Arrays.deepToString(grid));
//        print(grid, grid"newly_spanwed_2");
        return 0;

    }
    static boolean allTrue(boolean[] f){
        for (boolean i : f){
            if (!i) return false;
        }
        return true;
    }

    //DSU code
    static int getRoot(int index){
        if (root[index] == index) return index;
        int r = getRoot(root[index]);
        root[index] = r;
        return r;
    }
    static void merge(int i1, int i2){
//        System.out.println("merge called");
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
