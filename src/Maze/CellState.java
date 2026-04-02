package Maze;

/** 迷宫格子的状态枚举 */
public enum CellState {
    EMPTY,   // 普通空地
    WALL,    // 实体墙（外围/未打通）
    PLAYER,  // 玩家当前位置
    EXIT,    // 出口
    ITEM,    // 道具
    ENEMY    // 敌人
}
