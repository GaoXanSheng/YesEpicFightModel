package top.yunmouren.tools;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.function.Consumer;

public class MessageScreen extends Screen {
    private final String message;
    private final Screen previousScreen;
    private final Consumer<Button> okButton;
    private Consumer<Button> cancelButton =null;
    private final int width;
    private final int height;
    public MessageScreen(String titleKey, String messageKey, Screen previousScreen, Consumer<Button> okButton, Consumer<Button> cancelButton,int width, int height) {
        super(new TranslatableComponent(titleKey));
        this.message = messageKey;
        this.previousScreen = previousScreen;
        this.okButton = okButton;
        this.width = width;
        this.height = height;
        this.cancelButton = cancelButton;
    }
    public MessageScreen(String titleKey, String messageKey, Screen previousScreen, Consumer<Button> okButton,int width, int height) {
        super(new TranslatableComponent(titleKey));
        this.message = messageKey;
        this.previousScreen = previousScreen;
        this.okButton = okButton;
        this.width = width;
        this.height = height;
    }

    @Override
    protected void init() {

        // “确定”按钮
        this.addRenderableWidget(new Button(width, height, 100, 20, new TranslatableComponent("gui.ok"), (button) -> {
            if (okButton != null) {
                okButton.accept(button); // 执行回调
            }else {
                cancelButton.accept(button);
            }
        }));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, new TranslatableComponent(this.title.getString()), this.width / 2, this.height / 2 - 30, 0xFFFFFF);
        drawCenteredString(poseStack, this.font, new TranslatableComponent(this.message), this.width / 2, this.height / 2 - 10, 0xAAAAAA);
        super.render(poseStack, mouseX, mouseY, delta);
    }

}
