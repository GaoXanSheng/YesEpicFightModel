package com.p1nero.efmm.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.p1nero.efmm.client.gui.widget.TexturedModelPreviewer;
import com.p1nero.efmm.efmodel.ClientModelManager;
import com.p1nero.efmm.efmodel.ServerModelManager;
import com.p1nero.efmm.gameasstes.EFMMArmatures;
import io.netty.util.internal.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import top.yunmouren.tools.MessageScreen;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.MeshProvider;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.Armatures;

import java.util.Comparator;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class SelectModelToSendScreen extends Screen {
    private ModelList modelList;
    private TexturedModelPreviewer texturedModelPreviewer;
    private EditBox searchBox;
    private final Consumer<String> selectCallback;
    private final Consumer<String> cancelCallback;
    private final SelectEFModelScreen parent;
    private static final Logger LOGGER = LogUtils.getLogger();

    public SelectModelToSendScreen(SelectEFModelScreen parent, Consumer<String> selectCallback, Consumer<String> cancelCallback) {
        super(Component.nullToEmpty("gui.efmm.select.models"));
        this.parent = parent;
        this.selectCallback = selectCallback;
        this.cancelCallback = cancelCallback;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void refreshModelList() {
        this.modelList.refreshModelList(this.searchBox.getValue());
    }

    @Override
    protected void init() {
        this.texturedModelPreviewer = new TexturedModelPreviewer(10, 20, 36, 60, null, null, null, null);
        this.modelList = new ModelList(Minecraft.getInstance(), this.width, this.height, 36, this.height - 16, 21);
        this.modelList.setRenderTopAndBottom(false);
        this.searchBox = new EditBox(Minecraft.getInstance().font, this.width / 2, 12, this.width / 2 - 12, 16, Component.nullToEmpty("tip.efmm.select_ef_model.keyword"));
        this.searchBox.setResponder(this.modelList::refreshModelList);
        ClientModelManager.loadNativeModels();
        this.modelList.refreshModelList(null);

        int split = this.width / 2 - 80;

        this.texturedModelPreviewer._setWidth(split - 10);
        this.texturedModelPreviewer._setHeight(this.height - 68);
        this.texturedModelPreviewer.resize();

        this.modelList.updateSize(this.width - split, this.height, 36, this.height - 32);
        this.modelList.setLeftPos(split);

        this.searchBox.setX(this.width / 2);
        this.searchBox.y = (12);
        this.searchBox.setWidth(this.width / 2 - 12);
        this.searchBox.setHeight(16);

        this.addRenderableWidget(this.searchBox);
        this.addRenderableWidget(this.texturedModelPreviewer);
        this.addRenderableWidget(this.modelList);

        this.addRenderableWidget(new Button(this.width / 2 - 162, this.height - 28, 160, 21, CommonComponents.GUI_DONE, (button) -> {
            if (this.modelList.getSelected() == null) {
                Minecraft.getInstance().setScreen(new MessageScreen("", I18n.get("tip.efmm.please_select"), this, (button$2) -> {
                    Minecraft.getInstance().setScreen(this);
                }, 180, 60));
            } else {
                ModelList.ModelEntry modelEntry = this.modelList.getSelected();
                Minecraft.getInstance().setScreen(new MessageScreen("", I18n.get("tip.efmm.sure_to_send"), this, (okButton) -> {
                    try {
                        this.selectCallback.accept(modelEntry.modelId);
                        this.onClose();
                    } catch (Exception e) {
                        LOGGER.error("Error when upload model: ", e);
                        Minecraft.getInstance().setScreen(null);
                    }
                }, (cancelButton) -> {
                    Minecraft.getInstance().setScreen(this);
                }, 180, 60));
            }

        }));
        this.addRenderableWidget(new Button(this.width / 2 + 2, this.height - 28, 160, 21, CommonComponents.GUI_CANCEL, (button) -> {
            this.cancelCallback.accept(StringUtils.EMPTY);
            this.onClose();
            Minecraft.getInstance().setScreen(parent);
        }));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (this.texturedModelPreviewer.mouseDragged(mouseX, mouseY, button, dx, dy)) {
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
        this.texturedModelPreviewer.onDestroy();
    }

    @Override
    public void tick() {
        this.texturedModelPreviewer._tick();
    }
    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        super.render(poseStack, mouseX, mouseY, partialTicks);
        Component component = Component.nullToEmpty("tip.efmm.select_model_to_upload");
        float scale = Math.min(100f / font.width(component), 21f / font.lineHeight);
        poseStack.pushPose();
        poseStack.scale(scale, scale, 1.0f);
        drawString(poseStack,font, component, 50, 15, 16777215);
        poseStack.popPose();
    }
    @OnlyIn(Dist.CLIENT)
    class ModelList extends ObjectSelectionList<ModelList.ModelEntry> {
        
        public ModelList(Minecraft minecraft, int width, int height, int y0, int y1, int itemHeight) {
            super(minecraft, width, height, y0, y1, itemHeight);
            this.setRenderBackground(false);//泥土丑甚
        }

        @Override
        public void setSelected(@Nullable ModelEntry selEntry) {
            super.setSelected(selEntry);
            if(selEntry != null){
                SelectModelToSendScreen.this.texturedModelPreviewer.setModelId(selEntry.modelId);
                SelectModelToSendScreen.this.texturedModelPreviewer.setMesh(selEntry.mesh);
                SelectModelToSendScreen.this.texturedModelPreviewer.setArmature(EFMMArmatures.ARMATURES.getOrDefault(selEntry.modelId, Armatures.BIPED));
                SelectModelToSendScreen.this.texturedModelPreviewer.addAnimationToPlay(Animations.BIPED_WALK);
                SelectModelToSendScreen.this.texturedModelPreviewer.setTextureLocation(ClientModelManager.TEXTURE_CACHE.get(selEntry.modelId));
                SelectModelToSendScreen.this.texturedModelPreviewer.setAuthorName(() -> ClientModelManager.getLocalModelConfig(selEntry.modelId).getAuthorName());
            }
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.x1 - 6;
        }

        public void refreshModelList(String keyword) {
            this.setScrollAmount(0.0D);
            this.children().clear();
            //刷新模型列表
            ClientModelManager.LOCAL_MODELS.keySet().stream().filter((modelId) -> (StringUtil.isNullOrEmpty(keyword) || modelId.contains(keyword))).map((modelId) -> new ModelEntry(modelId, () -> ClientModelManager.getOrRequestMesh(modelId)))
                    .sorted(Comparator.comparing(entry$ -> entry$.modelId)).forEach(this::addEntry);
        }

        @OnlyIn(Dist.CLIENT)
        class ModelEntry extends Entry<ModelEntry> {
            private final String modelId;
            private final MeshProvider<AnimatedMesh> mesh;

            public ModelEntry(String modelId, MeshProvider<AnimatedMesh> mesh) {
                this.modelId = modelId;
                this.mesh = mesh;
            }
            @Override
            public void render(PoseStack poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
                drawString(poseStack,Minecraft.getInstance().font, this.modelId, left + 5, top + 5, 16777215);
            }

            @Override
            public @NotNull Component getNarration() {
                return Component.nullToEmpty("narrator.select");
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    if (ModelList.this.getSelected() == this) {
                        Minecraft.getInstance().setScreen(new MessageScreen("", I18n.get("tip.efmm.sure_to_send"), SelectModelToSendScreen.this, (okButton) -> {
                            try {
                                SelectModelToSendScreen.this.selectCallback.accept(this.modelId);
                                SelectModelToSendScreen.this.onClose();
                            } catch (Exception e) {
                                Minecraft.getInstance().setScreen(null);
                            }
                        }, (cancelButton) -> {
                                Minecraft.getInstance().setScreen(SelectModelToSendScreen.this);
                        }, 180, 60));

                        return true;
                    }

                    ModelList.this.setSelected(this);

                    return true;
                } else {
                    return false;
                }
            }


        }
    }
}
