package org.passerbya.ui;

/**
 * 页面标识。id 决定切换动画的方向。
 *
 * <p>id 的取值要和左侧菜单栏从上到下的顺序一致：菜单是竖直排列的，
 * 所以 "往下走" 就对应 id 递增、"往上走" 对应 id 递减。
 * 新增页面时插在对应位置并保持 id 连续即可，动画方向会自动跟着对。
 *
 * <p>这里用显式数值而不是 {@code ordinal()}，避免以后有人调整枚举声明顺序
 * 时把动画方向悄悄改掉。
 */
public enum PageId {

    HOME(0),
    TERMINAL(1),
    PLUGINS(2),
    SETTINGS(3);

    private final int id;

    PageId(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }
}
