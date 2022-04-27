package game2048;

import java.util.Formatter;
import java.util.Observable;


/** The state of a game of 2048.
 *  @author FlyingRain
 */
public class Model extends Observable {
    /** Current contents of the board. */
    private Board board;
    /** Current score. */
    private int score;
    /** Maximum score so far.  Updated when game ends. */
    private int maxScore;
    /** True iff game is ended. */
    private boolean gameOver;

    /* Coordinate System: column C, row R of the board (where row 0,
     * column 0 is the lower-left corner of the board) will correspond
     * to board.tile(c, r).  Be careful! It works like (x, y) coordinates.
     */

    /** Largest piece value. */
    public static final int MAX_PIECE = 2048;

    /** A new 2048 game on a board of size SIZE with no pieces
     *  and score 0. */
    public Model(int size) {
        board = new Board(size);
        score = maxScore = 0;
        gameOver = false;
    }

    /** A new 2048 game where RAWVALUES contain the values of the tiles
     * (0 if null). VALUES is indexed by (row, col) with (0, 0) corresponding
     * to the bottom-left corner. Used for testing purposes. */
    public Model(int[][] rawValues, int score, int maxScore, boolean gameOver) {
        int size = rawValues.length;
        board = new Board(rawValues, score);
        this.score = score;
        this.maxScore = maxScore;
        this.gameOver = gameOver;
    }

    /** Return the current Tile at (COL, ROW), where 0 <= ROW < size(),
     *  0 <= COL < size(). Returns null if there is no tile there.
     *  Used for testing. Should be deprecated and removed.
     *  */
    public Tile tile(int col, int row) {
        return board.tile(col, row);
    }

    /** Return the number of squares on one side of the board.
     *  Used for testing. Should be deprecated and removed. */
    public int size() {
        return board.size();
    }

    /** Return true iff the game is over (there are no moves, or
     *  there is a tile with value 2048 on the board). */
    public boolean gameOver() {
        checkGameOver();
        if (gameOver) {
            maxScore = Math.max(score, maxScore);
        }
        return gameOver;
    }

    /** Return the current score. */
    public int score() {
        return score;
    }

    /** Return the current maximum game score (updated at end of game). */
    public int maxScore() {
        return maxScore;
    }

    /** Clear the board to empty and reset the score. */
    public void clear() {
        score = 0;
        gameOver = false;
        board.clear();
        setChanged();
    }

    /** Add TILE to the board. There must be no Tile currently at the
     *  same position. */
    public void addTile(Tile tile) {
        board.addTile(tile);
        checkGameOver();
        setChanged();
    }

    /**
     * 记录每个方块在每次操作时是否进行过合并。即方块移动追踪数组
     */
    private int[][] moveTrace;

    /**
     * 初始化方块移动追踪数组
     * @param length 数组的长度
     */
    private void initMoveTrace(int length) {
        // 如果一开始没有初始化，则先进行初始化
        if (moveTrace == null) {
            moveTrace = new int[length][length];
        }
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                moveTrace[i][j] = 0;
            }
        }
    }

    /** Tilt the board toward SIDE. Return true iff this changes the board.
     *
     * 1. If two Tile objects are adjacent in the direction of motion and have
     *    the same value, they are merged into one Tile of twice the original
     *    value and that new value is added to the score instance variable
     * 2. A tile that is the result of a merge will not merge again on that
     *    tilt. So each move, every tile will only ever be part of at most one
     *    merge (perhaps zero).
     * 3. When three adjacent tiles in the direction of motion have the same
     *    value, then the leading two tiles in the direction of motion merge,
     *    and the trailing tile does not.
     * */
    public boolean tilt(Side side) {
        boolean changed;
        changed = false;
        // TODO: Modify this.board (and perhaps this.score) to account
        // for the tilt to the Side SIDE. If the board changed, set the
        // changed local variable to true.

        Board board = this.board;
        int length = board.size();
        initMoveTrace(length);
        // 1. 首先考虑上移
        if (side.equals(Side.NORTH)) {
            changed = moveUp();
        }
        else {
            board.setViewingPerspective(side);
            changed = moveUp();
            board.setViewingPerspective(Side.NORTH);
        }
        checkGameOver();
        if (changed) {
            setChanged();
        }
        return changed;
    }

    private boolean moveUp() {
        boolean changed = false;
        Board board = this.board;
        int length = board.size();
        initMoveTrace(length);
        // 遍历全图
        for (int col = 0; col < length; col++) {
            // 从最顶层开始
            for (int row = length - 1; row >= 0; row--) {
                Tile tile = board.tile(col, row);
                // 1. 如果当前方块是空，则不作操作
                if (tile != null) {
                    // 2. 找到上方的最高层
                    int lastPosition = findLastPosition(board, col, row);
                    // 2.1 如果当前层就是最高层，则无操作
                    if (lastPosition != row ) {
                        changed = true;
                        boolean mergeFlag = board.move(col, lastPosition, tile);
                        if (mergeFlag) {
                            score += tile.value() * 2;
                            moveTrace[col][lastPosition] = 1;
                        }
                    }
                }
            }
        }
        return changed;
    }

    private int findLastPosition(Board board, int col, int row) {
        int length = board.size();
        Tile currentTile = board.tile(col, row);
        int currentValue = currentTile.value();
        int lastPos = 0;
        // 从 row 的上一层开始
        lastPos = row;
        for (int i = row + 1; i < length; i++) {
            Tile tile = board.tile(col, i);
            lastPos = i;
            // 如果是空的，则继续往上找
            if (tile == null) {
                continue;
            }
            if (tile.value() != currentValue || moveTrace[col][lastPos] != 0) {
                lastPos -= 1;
            }
            break;
        }
        return lastPos;
    }


    /** Checks if the game is over and sets the gameOver variable
     *  appropriately.
     */
    private void checkGameOver() {
        gameOver = checkGameOver(board);
    }

    /** Determine whether game is over. */
    private static boolean checkGameOver(Board b) {
        return maxTileExists(b) || !atLeastOneMoveExists(b);
    }

    /** Returns true if at least one space on the Board is empty.
     *  Empty spaces are stored as null.
     * */
    public static boolean emptySpaceExists(Board b) {
        // TODO: Fill in this function.
        int length = b.size();
        // 遍历全图
        for (int col = 0; col < length; col++) {
            for (int row = 0; row < length; row++) {
                // 如果其中一个方块为空，则证明存在空方块
                if (b.tile(col, row) == null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if any tile is equal to the maximum valid value.
     * Maximum valid value is given by MAX_PIECE. Note that
     * given a Tile object t, we get its value with t.value().
     */
    public static boolean maxTileExists(Board b) {
        // TODO: Fill in this function.
        int length = b.size();
        // 遍历全图
        for (int col = 0; col < length; col++) {
            for (int row = 0; row < length; row++) {
                Tile tile = b.tile(col, row);
                // 如果其中一个方块的值为2048，则证明存在最大的方块
                if (tile != null && tile.value() == MAX_PIECE) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if there are any valid moves on the board.
     * There are two ways that there can be valid moves:
     * 1. There is at least one empty space on the board.
     * 2. There are two adjacent tiles with the same value.
     */
    public static boolean atLeastOneMoveExists(Board b) {
        // TODO: Fill in this function.
        if (emptySpaceExists(b)) {
            return true;
        }
        int length = b.size();
        // 遍历全图
        for (int col = 0; col < length; col++) {
            for (int row = 0; row < length; row++) {
                Tile tile = b.tile(col, row);
                int value = tile.value();
                // 2. 寻找其相邻的方块，并验证其值是否相等
                // 2.1 左边方块
                if (row - 1 >= 0 && b.tile(col, row - 1).value() == value) {
                    return true;
                }
                // 2.2 右边方块
                if (row + 1 < length && b.tile(col, row + 1).value() == value) {
                    return true;
                }
                // 2.3 上边方块
                if (col - 1 >= 0 && b.tile(col - 1, row).value() == value) {
                    return true;
                }
                // 2.4 下边方块
                if (col + 1 < length && b.tile(col + 1, row).value() == value) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
     /** Returns the model as a string, used for debugging. */
    public String toString() {
        Formatter out = new Formatter();
        out.format("%n[%n");
        for (int row = size() - 1; row >= 0; row -= 1) {
            for (int col = 0; col < size(); col += 1) {
                if (tile(col, row) == null) {
                    out.format("|    ");
                } else {
                    out.format("|%4d", tile(col, row).value());
                }
            }
            out.format("|%n");
        }
        String over = gameOver() ? "over" : "not over";
        out.format("] %d (max: %d) (game is %s) %n", score(), maxScore(), over);
        return out.toString();
    }

    @Override
    /** Returns whether two models are equal. */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (getClass() != o.getClass()) {
            return false;
        } else {
            return toString().equals(o.toString());
        }
    }

    @Override
    /** Returns hash code of Model’s string. */
    public int hashCode() {
        return toString().hashCode();
    }
}
