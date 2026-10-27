package com.galtc2.compat.littlemaid;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.galtc2.compat.littlemaid.tinkers.TinkersBowMaidTask;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * 车万女仆 的官方扩展入口（{@code @LittleMaidExtension} + {@link ILittleMaid}）。
 * <p>
 * 女仆模组会扫描带 {@code @LittleMaidExtension} 注解的类并调用 {@link ILittleMaid} 的默认方法，
 * 这是给第三方注册「工作模式 / 女仆饰品 / 背包 / 记忆类型 / AI 行为」的官方途径
 * （见女仆文档「添加新的工作模式」）。
 * <p>
 * 只有装了车万女仆 时这个类才会被扫描/加载；未装女仆时它不会被任何人触碰。
 * 内部再判断匠魂是否存在，避免匠魂缺席时加载 {@link TinkersBowMaidTask}。
 */
@LittleMaidExtension
public class Galtc2MaidExtension implements ILittleMaid {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String TCONSTRUCT_MOD_ID = "tconstruct";

    @Override
    public void addMaidTask(TaskManager manager) {
        if (!ModList.get().isLoaded(TCONSTRUCT_MOD_ID)) {
            LOGGER.info("[galtc2] 未检测到匠魂，跳过「匠魂弓术」女仆任务。");
            return;
        }
        manager.add(new TinkersBowMaidTask());
        LOGGER.info("[galtc2] 已通过女仆扩展接口注册工作模式：匠魂弓术 ({})",
                TinkersBowMaidTask.UID);
    }
}
