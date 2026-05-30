import java.util.Random;
import java.util.Scanner;

/**
 * 扫雷游戏 (Minesweeper Game)
 * 
 * 这是一个控制台版本的扫雷游戏，包含完整的游戏逻辑。
 * 功能包括：
 * - 首次点击安全（第一次翻格子绝对不会踩到雷）
 * - 数字自动展开（当翻开空白格时自动展开周围区域）
 * - 标记/取消标记地雷
 * - 胜利/失败判定
 * - 计时功能
 */
public class Minesweeper {

    // ======================== 游戏常量 ========================
    private static final int ROWS = 9;         // 行数
    private static final int COLS = 9;         // 列数
    private static final int MINE_COUNT = 10;  // 地雷数量
    private static final char MINE = '*';      // 地雷显示字符
    private static final char FLAG = '⚑';      // 旗帜显示字符
    private static final char UNOPENED = '■';  // 未翻开格子显示字符
    private static final char EMPTY = ' ';     // 空格显示字符

    // ======================== 游戏状态变量 ========================
    private static char[][] board;       // 玩家看到的游戏面板
    private static boolean[][] mines;    // 地雷位置矩阵 (true=有雷)
    private static boolean[][] opened;   // 是否已翻开
    private static boolean[][] flagged;  // 是否已标记旗帜
    private static boolean gameOver;     // 游戏是否结束
    private static boolean firstClick;   // 是否是第一次点击
    private static int cellsToOpen;      // 需要翻开的格子数（用于判断胜利）
    private static int flagsPlaced;      // 已标记的旗帜数
    private static long startTime;       // 游戏开始时间
    private static Scanner scanner;      // 输入扫描器

    /**
     * 程序入口 - 主方法
     */
    public static void main(String[] args) {
        scanner = new Scanner(System.in);
        boolean playAgain;

        // 显示游戏标题
        System.out.println("=================================");
        System.out.println("         扫  雷  游  戏          ");
        System.out.println("=================================");
        System.out.println("  行数: " + ROWS + "  列数: " + COLS + "  雷数: " + MINE_COUNT);
        System.out.println("=================================");

        // 游戏主循环 - 允许反复重玩
        do {
            initGame();           // 初始化游戏
            playGame();           // 开始游戏
            System.out.print("\n是否再玩一局？(y/n): ");
            playAgain = scanner.next().toLowerCase().charAt(0) == 'y';
        } while (playAgain);

        System.out.println("感谢游玩！再见！");
        scanner.close();
    }

    /**
     * 初始化游戏 - 重置所有数据结构
     * 此时不放置地雷，等第一次点击时才放（保证安全）
     */
    private static void initGame() {
        board = new char[ROWS][COLS];
        mines = new boolean[ROWS][COLS];
        opened = new boolean[ROWS][COLS];
        flagged = new boolean[ROWS][COLS];

        // 初始化显示面板为未翻开状态
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                board[r][c] = UNOPENED;
            }
        }

        gameOver = false;
        firstClick = true;      // 标记为首次点击
        flagsPlaced = 0;
        cellsToOpen = ROWS * COLS - MINE_COUNT;  // 需要翻开的安全格数
        startTime = 0;
    }

    /**
     * 游戏主逻辑循环
     * 不断接收玩家输入直到游戏结束
     */
    private static void playGame() {
        while (!gameOver) {
            printBoard();           // 打印当前面板
            printStatus();          // 打印状态信息
            String input = getInput();  // 获取玩家输入

            if (input == null) continue;  // 输入无效，重新输入

            // 解析输入: 格式为 "操作 行 列"
            // 操作: o=翻开, f=标记/取消标记旗帜
            String[] parts = input.split(" ");
            char action = parts[0].charAt(0);
            int row = Integer.parseInt(parts[1]) - 1;  // 转为0索引
            int col = Integer.parseInt(parts[2]) - 1;  // 转为0索引

            // 检查坐标是否越界
            if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
                System.out.println("❌ 坐标超出范围！请输入 1~" + ROWS + " 之间的数字。");
                continue;
            }

            // 如果格子已经翻开，忽略操作
            if (opened[row][col]) {
                System.out.println("⚠️ 该格子已经翻开了！");
                continue;
            }

            // 根据操作类型执行不同逻辑
            switch (action) {
                case 'o':  // 翻开格子
                    handleOpen(row, col);
                    break;
                case 'f':  // 标记/取消标记旗帜
                    handleFlag(row, col);
                    break;
                default:
                    System.out.println("❌ 未知操作！请输入 o(翻开) 或 f(标记)");
            }
        }

        // 游戏结束，显示最终结果
        printBoard(true);  // 显示所有地雷位置
        if (isWin()) {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            System.out.println("🎉 恭喜你赢了！用时 " + elapsed + " 秒！");
        } else {
            System.out.println("💥 踩到地雷了！游戏结束！");
        }
    }

    /**
     * 处理翻开格子的操作
     * 如果是第一次点击，先放置地雷（保证当前位置安全）
     */
    private static void handleOpen(int row, int col) {
        // 有旗帜的格子不能翻开，必须先取消旗帜
        if (flagged[row][col]) {
            System.out.println("⚠️ 该位置有旗帜，请先用 f 取消标记！");
            return;
        }

        // === 首次点击安全机制 ===
        // 第一次点击时再放置地雷，并确保点击位置及其周围没有雷
        if (firstClick) {
            placeMines(row, col);     // 放雷（避开点击位置）
            startTime = System.currentTimeMillis();  // 开始计时
            firstClick = false;
        }

        // 踩到地雷，游戏结束
        if (mines[row][col]) {
            gameOver = true;
            return;
        }

        // 翻开当前格子
        openCell(row, col);
    }

    /**
     * 处理标记/取消标记旗帜的操作
     * 只能对未翻开的格子进行操作
     */
    private static void handleFlag(int row, int col) {
        if (flagged[row][col]) {
            // 取消旗帜标记
            flagged[row][col] = false;
            board[row][col] = UNOPENED;  // 恢复未翻开显示
            flagsPlaced--;
            System.out.println("⛳ 已取消标记 (" + (row+1) + "," + (col+1) + ")");
        } else {
            // 标记旗帜（不能超过地雷总数）
            if (flagsPlaced < MINE_COUNT) {
                flagged[row][col] = true;
                board[row][col] = FLAG;
                flagsPlaced++;
                System.out.println("🚩 已标记 (" + (row+1) + "," + (col+1) + ")");
            } else {
                System.out.println("❌ 旗帜已用完！（最多 " + MINE_COUNT + " 个）");
            }
        }
    }

    /**
     * 放置地雷 - 确保首次点击位置及其周围8格绝对安全
     * 使用 Fisher-Yates 洗牌算法思想随机放置
     * 
     * @param safeRow 安全行（首次点击行）
     * @param safeCol 安全列（首次点击列）
     */
    private static void placeMines(int safeRow, int safeCol) {
        Random rand = new Random();
        int placed = 0;

        while (placed < MINE_COUNT) {
            int r = rand.nextInt(ROWS);
            int c = rand.nextInt(COLS);

            // 如果该位置已经有雷，跳过
            if (mines[r][c]) continue;

            // === 安全区域检查 ===
            // 确保点击位置及周围8格没有雷
            boolean isSafeZone = Math.abs(r - safeRow) <= 1 && Math.abs(c - safeCol) <= 1;
            if (isSafeZone) continue;

            // 放置地雷
            mines[r][c] = true;
            placed++;
        }
    }

    /**
     * 翻开指定格子，如果是空格则自动展开（Flood Fill 算法）
     * 这是扫雷游戏的核心算法之一
     * 
     * @param row 行索引
     * @param col 列索引
     */
    private static void openCell(int row, int col) {
        // 边界检查，或者已经翻开/有旗帜，则返回
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return;
        if (opened[row][col] || flagged[row][col]) return;
        if (mines[row][col]) return;  // 有雷不翻开（防止递归踩雷）

        // 标记为已翻开
        opened[row][col] = true;

        // 计算周围地雷数量
        int mineCount = countAdjacentMines(row, col);

        if (mineCount > 0) {
            // 周围有雷，显示数字（不同颜色区域用数字表示）
            board[row][col] = (char) ('0' + mineCount);
        } else {
            // === 核心：自动展开 ===
            // 周围没有雷，显示空白，并递归翻开周围8个格子
            board[row][col] = EMPTY;

            // 递归展开周围8个方向
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;  // 跳过自己
                    openCell(row + dr, col + dc);
                }
            }
        }
    }

    /**
     * 计算指定格子周围8格的地雷数量
     * 
     * @param row 行索引
     * @param col 列索引
     * @return 周围地雷数量 (0~8)
     */
    private static int countAdjacentMines(int row, int col) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int r = row + dr;
                int c = col + dc;
                // 边界检查
                if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                    if (mines[r][c]) count++;
                }
            }
        }
        return count;
    }

    /**
     * 检查玩家是否获胜
     * 当所有非地雷格子都被翻开时获胜
     */
    private static boolean isWin() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                // 如果有非地雷格子未翻开，则还没赢
                if (!mines[r][c] && !opened[r][c]) {
                    return false;
                }
            }
        }
        return true;
    }

    // ======================== 输入输出方法 ========================

    /**
     * 获取玩家输入
     * 格式: o 行 列  (翻开)
     *       f 行 列  (标记旗帜)
     * 行和列从1开始计数
     */
    private static String getInput() {
        System.out.print("\n请输入操作 (o=翻开 / f=标记旗帜) 行 列，例如: o 3 5\n>>> ");
        String input = scanner.nextLine().trim();

        // 空输入则重新提示
        if (input.isEmpty()) {
            input = scanner.nextLine().trim();
        }

        // 检查格式: 操作符 + 行 + 列
        String[] parts = input.split(" ");
        if (parts.length != 3) {
            System.out.println("❌ 格式错误！正确格式: o 行 列  或  f 行 列");
            return null;
        }

        // 验证操作符
        String action = parts[0].toLowerCase();
        if (!action.equals("o") && !action.equals("f")) {
            System.out.println("❌ 操作符错误！o=翻开, f=标记旗帜");
            return null;
        }

        // 验证行列是数字
        try {
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);
            if (row < 1 || row > ROWS || col < 1 || col > COLS) {
                System.out.println("❌ 坐标超出范围！请输入 1~" + ROWS + " 之间的数字。");
                return null;
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ 行和列必须是数字！");
            return null;
        }

        return input;
    }

    /**
     * 打印游戏状态信息（地雷数、旗帜数、用时等）
     */
    private static void printStatus() {
        System.out.println("---------------------------------");
        System.out.println("  地雷: " + MINE_COUNT + "  旗帜: " + flagsPlaced + "/" + MINE_COUNT);
        if (startTime > 0) {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            System.out.println("  用时: " + elapsed + " 秒");
        }
        System.out.println("---------------------------------");
    }

    /**
     * 打印游戏面板（玩家视角，不显示地雷位置）
     * 调用无参版本，默认不显示地雷
     */
    private static void printBoard() {
        printBoard(false);
    }

    /**
     * 打印游戏面板
     * 
     * @param showMines 是否显示地雷位置（游戏结束时为true）
     */
    private static void printBoard(boolean showMines) {
        System.out.println();
        System.out.print("   ");  // 列号前缀缩进

        // 打印列号（从1开始，方便玩家对应）
        for (int c = 0; c < COLS; c++) {
            System.out.print(" " + (c + 1) + " ");
        }
        System.out.println();

        // 打印每一行
        for (int r = 0; r < ROWS; r++) {
            // 打印行号
            System.out.print((r + 1) + "  ");

            // 打印每一列
            for (int c = 0; c < COLS; c++) {
                char displayChar;

                if (showMines && mines[r][c]) {
                    // 游戏结束显示模式: 显示所有地雷
                    displayChar = MINE;
                } else if (flagged[r][c]) {
                    // 显示旗帜
                    displayChar = FLAG;
                } else if (!opened[r][c]) {
                    // 未翻开
                    displayChar = UNOPENED;
                } else {
                    // 已翻开
                    displayChar = board[r][c];
                }

                System.out.print(" " + displayChar + " ");
            }
            System.out.println();  // 换行
        }
        System.out.println();
    }
}
