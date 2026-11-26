package com.bettergi.pocket.input
/**
sealed interface AutomationAction
密封接口，表示“自动化动作”这个封闭的类型集合。所有实现必须和它写在同一个文件/模块里，编译器知道全部可能的动作类型。
为什么用 sealed 而不是普通 interface：
 动作类型是有限的、已知的（点击、滑动、按键等）；
 调用方用 when 处理时可以穷尽，漏一种编译报错
*/
sealed interface AutomationAction

data class ClickAction(
    val x: Int,
    val y: Int,
    val durationMs: Long = 50L,
) : AutomationAction

//接收任意 AutomationAction，负责真正执行这个动作
interface AutomationController {
    fun execute(action: AutomationAction)
}

object NoOpAutomationController : AutomationController {
    override fun execute(action: AutomationAction) = Unit //空实现，真正实现在无障碍权限给了之后
}
//ActionEmitter产生动作，AutomationController执行动作
interface ActionEmitter {
    fun emit(action: AutomationAction)
}
