package com.p1nero.efmm.client.gui.widget;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.math.Matrix4f;
import com.p1nero.efmm.efmodel.ClientModelManager;
import com.p1nero.efmm.gameasstes.EFMMArmatures;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Keyframe;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.TransformSheet;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.LayerOffAnimation;
import yesman.epicfight.api.animation.types.LinkAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.MeshProvider;
import yesman.epicfight.api.client.model.RawMesh;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.CubicBezierCurve;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.QuaternionUtils;
import yesman.epicfight.client.gui.datapack.widgets.CheckBox;
import yesman.epicfight.client.gui.datapack.widgets.ResizableComponent;
import yesman.epicfight.client.particle.TrailParticle;
import yesman.epicfight.client.renderer.EpicFightShaders;
import yesman.epicfight.config.EpicFightOptions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.StunType;

@OnlyIn(Dist.CLIENT)
public class TexturedModelPreviewer extends AbstractWidget implements ResizableComponent {
    private String modelId;
    private Supplier<String> authorName = null;
    private NoEntityAnimator animator;
    private final ModelRenderTarget modelRenderTarget;
    private final List<StaticAnimation> animationsToPlay = Lists.newArrayList();
    private final List<CustomTrailParticle> trailParticles = Lists.newArrayList();
    private final CheckBox showColliderCheckbox = new CheckBox(Minecraft.getInstance().font, 0, 60, 0, 10, null, null, true, Component.nullToEmpty("datapack_edit.model_player.collider"), null);
    private final CheckBox showItemCheckbox = new CheckBox(Minecraft.getInstance().font, 0, 40, 0, 10, null, null, true, Component.nullToEmpty("datapack_edit.model_player.item"), null);
    private final CheckBox showTrailCheckbox = new CheckBox(Minecraft.getInstance().font, 0, 40, 0, 10, null, null, true, Component.nullToEmpty("datapack_edit.model_player.trail"), null);
    private double zoom = -5.0D;
    private float xRot = 0.0F;
    private float yRot = 180.0F;
    private float xMove = 0.0F;
    private float yMove = 0.0F;
    private int index;
    private MeshProvider<AnimatedMesh> mesh;
    @Nullable
    private ResourceLocation textureLocation;
    private Collider collider;
    private final List<TrailInfo> trailInfoList = Lists.newArrayList();
    private Item item;

    public TexturedModelPreviewer(int x1, int x2, int y1, int y2, HorizontalSizing horizontal, VerticalSizing vertical, Armature armature, MeshProvider<AnimatedMesh> mesh) {
        super(x1, y1, x2, y2, Component.nullToEmpty(""));

        if (armature != null) {
            FakeEntityPatch patch = new FakeEntityPatch(armature);
            this.animator = new NoEntityAnimator(patch);
            patch.setAnimator();
        }

        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.horizontalSizingOption = horizontal;
        this.verticalSizingOption = vertical;
        this.mesh = mesh;

        this.modelRenderTarget = new ModelRenderTarget();
        this.resize();

        this.modelRenderTarget.setClearColor(0.1552F, 0.1552F, 0.1552F, 1.0F);
        this.modelRenderTarget.clear(Minecraft.ON_OSX);
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setAuthorName(Supplier<String> authorName) {
        this.authorName = authorName;
    }

    public String getAuthorName() {
        return authorName == null ? "" : authorName.get();
    }

    public void setArmature(Armature armature) {
        FakeEntityPatch patch = new FakeEntityPatch(armature);
        this.animator = new NoEntityAnimator(patch);
        patch.setAnimator();
    }

    public void setMesh(MeshProvider<AnimatedMesh> mesh) {
        this.mesh = mesh;
    }

    public void setTextureLocation(@Nullable ResourceLocation textureLocation) {
        this.textureLocation = textureLocation;
    }

    public @Nullable ResourceLocation getTextureLocation() {
        return ClientModelManager.TEXTURE_CACHE.get(modelId);
    }

    public Armature getArmature() {
        return EFMMArmatures.ARMATURES.getOrDefault(this.modelId, Armatures.BIPED);
    }

    public MeshProvider<AnimatedMesh> getMesh() {
        return this.mesh;
    }

    public NoEntityAnimator getAnimator() {
        return this.animator;
    }

    public void setCollider(Collider collider) {
        this.collider = collider;
    }

    public void setTrailInfo(TrailInfo... trailInfos) {
        this.trailInfoList.clear();

        this.trailInfoList.addAll(Arrays.asList(trailInfos));
    }

    public void setItemToRender(Item item) {
        this.item = item;
    }

    public void addAnimationToPlay(StaticAnimation animation) {
        if (this.index == -1) {
            this.index = 0;
        }

        this.animationsToPlay.add(animation);
        this.animator.playAnimation(animation, 0.0F);
    }

    public void removeAnimationPlayingAnimation(StaticAnimation animation) {
        this.animationsToPlay.remove(animation);
    }

    public void clearAnimations() {
        this.index = -1;
        this.animationsToPlay.clear();
        this.animator.playAnimation(Animations.DUMMY_ANIMATION, 0.0F);

        this.animator.getAllLayers().forEach((layer) -> {
            layer.off(this.animator.getEntityPatch());
        });

        this.animator.playAnimation(Animations.OFF_ANIMATION_HIGHEST, 0.0F);
        this.animator.playAnimation(Animations.OFF_ANIMATION_MIDDLE, 0.0F);
        this.animator.playAnimation(Animations.OFF_ANIMATION_LOWEST, 0.0F);
    }

    public void restartAnimations() {
        this.index = 0;

        if (this.animationsToPlay.isEmpty()) {
            this.index = -1;
            return;
        }

        this.animator.playAnimation(this.animationsToPlay.get(0), 0.0F);
    }

    @Override
    public void _tick() {
        if (this.animator != null) {
            this.animator.tick();
        }

        this.trailParticles.forEach(CustomTrailParticle::tick);
        this.trailParticles.removeIf((trail) -> !trail.isAlive());
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (this.active && this.visible) {
            if (this.isValidClickButton(button)) {
                boolean flag = this.clicked(x, y);

                if (flag) {
                    this.playDownSound(Minecraft.getInstance().getSoundManager());

                    if (this.item != null) {
                        if (this.showItemCheckbox.mouseClicked(x, y, button)) {
                            return true;
                        }
                    }

                    if (!this.trailInfoList.isEmpty()) {
                        if (this.showTrailCheckbox.mouseClicked(x, y, button)) {
                            return true;
                        }
                    }

                    if (this.collider != null) {
                        if (this.showColliderCheckbox.mouseClicked(x, y, button)) {
                            return true;
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (this.isMouseOver(mouseX, mouseY)) {
            if (button == 0) {
                this.xRot = (float)Mth.clamp(this.xRot + dy * 2.5D, -180.0D, 180.0D);
                this.yRot += dx * 2.5D;
            } else if (button == 2) {
                this.xMove += (float)dx * 0.015F * -this.zoom;
                this.yMove += -(float)dy * 0.015F * -this.zoom;
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double amount) {
        this.zoom = Mth.clamp(this.zoom + amount * 0.5D, -20.0D, -0.5D);
        return true;
    }
    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        partialTicks = minecraft.getFrameTime();
        minecraft.getMainRenderTarget().unbindWrite();

        Rect2i screenrectangle = null;
        boolean scissorApplied = !guiGraphics.scissorStack.stack.isEmpty();

        // If scissor test is enabled, remove it.
        if (scissorApplied) {
            screenrectangle = guiGraphics.scissorStack.stack.peekLast();
            guiGraphics.disableScissor();
        }

        this.modelRenderTarget.clear(true);
        this.modelRenderTarget.bindWrite(true);

        if(!this.getAuthorName().isEmpty()){
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(2.0F, 1.0F, 1.0F);
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tip.efmm.author_name").append(Component.literal(authorName.get())).withStyle(ChatFormatting.BOLD), this._getX(), this._getY() - 25, 16777215, true);
            guiGraphics.pose().popPose();
        }

        if(this.getMesh() != null && this.getMesh().get() != null){
            if (this.animator != null && getTextureLocation() != null) {
                Pose pose = this.animator.getPose(partialTicks);
                OpenMatrix4f[] poseMatrices = this.getArmature().getPoseAsTransformMatrix(pose, false);
                guiGraphics.pose().pushPose();

                ShaderInstance prevShader = RenderSystem.getShader();
                Matrix4f oldProjection = RenderSystem.getProjectionMatrix();
                Matrix4f perspective = (new Matrix4f()).setPerspective(70.0F, (float)this.width / (float)this.height, 0.05F, 100.0F);

                RenderSystem.setProjectionMatrix(perspective, VertexSorting.DISTANCE_TO_ORIGIN);
                RenderSystem.getModelViewStack().pushPose();
                RenderSystem.getModelViewStack().setIdentity();
                RenderSystem.applyModelViewMatrix();

                guiGraphics.pose().translate(this.xMove, this.yMove - 1.0D, this.zoom);
                guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(this.xRot));
                guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(this.yRot));

                this.mesh.get().initialize();
                RenderSystem.setShaderTexture(0, getTextureLocation());
                RenderSystem.setShader(GameRenderer::getRendertypeEntityTranslucentCullShader);
                Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
                Tesselator tesselator = RenderSystem.renderThreadTesselator();
                BufferBuilder bufferbuilder = tesselator.getBuilder();
                bufferbuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.NEW_ENTITY);
                this.mesh.get().drawToBuffer(guiGraphics.pose(), bufferbuilder, Mesh.DrawingFunction.ENTITY_TEXTURED, LightTexture.FULL_BRIGHT, 0.9411F, 0.9411F, 0.9411F, 1.0F, OverlayTexture.NO_OVERLAY, this.getArmature(), poseMatrices);
                BufferUploader.drawWithShader(bufferbuilder.end());
                Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();

                RenderSystem.setProjectionMatrix(oldProjection, VertexSorting.ORTHOGRAPHIC_Z);
                RenderSystem.getModelViewStack().popPose();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setShader(() -> prevShader);
                guiGraphics.pose().popPose();
            } else {
                guiGraphics.pose().pushPose();

                ShaderInstance prevShader = RenderSystem.getShader();
                Matrix4f oldProjection = RenderSystem.getProjectionMatrix();
                if(getTextureLocation() != null){
                    RenderSystem.setShaderTexture(0, getTextureLocation());
                    RenderSystem.setShader(GameRenderer::getRendertypeEntityAlphaShader);
                } else {
                    RenderSystem.setShader(EpicFightShaders::getPositionColorNormalShader);
                }

                Matrix4f perspective = (new Matrix4f()).setPerspective(70.0F, (float)this.width / (float)this.height, 0.05F, 100.0F);

                RenderSystem.setProjectionMatrix(perspective, VertexSorting.DISTANCE_TO_ORIGIN);
                RenderSystem.getModelViewStack().pushPose();
                RenderSystem.getModelViewStack().setIdentity();
                RenderSystem.applyModelViewMatrix();

                guiGraphics.pose().translate(this.xMove, this.yMove - 1.0D, this.zoom);
                guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(this.xRot));
                guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(this.yRot));

                this.mesh.get().initialize();
                Tesselator tesselator = RenderSystem.renderThreadTesselator();
                BufferBuilder bufferbuilder = tesselator.getBuilder();
                bufferbuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
                this.mesh.get().draw(guiGraphics.pose(), bufferbuilder, getTextureLocation() == null ? RawMesh.DrawingFunction.ENTITY_SOLID : RawMesh.DrawingFunction.ENTITY_TEXTURED, -1, 0.9411F, 0.9411F, 0.9411F, 1.0F, OverlayTexture.NO_OVERLAY);
                BufferUploader.drawWithShader(bufferbuilder.end());
                guiGraphics.pose().popPose();

                RenderSystem.setProjectionMatrix(oldProjection, VertexSorting.ORTHOGRAPHIC_Z);
                RenderSystem.getModelViewStack().popPose();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setShader(() -> prevShader);
            }
        }


        this.modelRenderTarget.unbindWrite();

        minecraft.getMainRenderTarget().bindWrite(true);

        if (scissorApplied) {
            guiGraphics.enableScissor(screenrectangle.left(), screenrectangle.top(), screenrectangle.right(), screenrectangle.bottom());
        }

        this.modelRenderTarget.blitToScreen(poseStack);

        if (this.animator != null) {
            // Visibility control widgets
            int top = this._getY() + 6;
            int right = this._getX() + this._getWidth() - 2;

            if (!this.trailInfoList.isEmpty()) {
                right -= this.showTrailCheckbox._getWidth();

                this.showTrailCheckbox._setX(right);
                this.showTrailCheckbox._setY(top);
                this.showTrailCheckbox._renderWidget(poseStack, mouseX, mouseY, partialTicks);
            }

            if (this.item != null) {
                right -= this.showItemCheckbox._getWidth();

                this.showItemCheckbox._setX(right);
                this.showItemCheckbox._setY(top);
                this.showItemCheckbox._renderWidget(poseStack, mouseX, mouseY, partialTicks);
            }

            if (this.collider != null) {
                right -= this.showColliderCheckbox._getWidth();

                this.showColliderCheckbox._setX(right);
                this.showColliderCheckbox._setY(top);
                this.showColliderCheckbox._renderWidget(poseStack, mouseX, mouseY, partialTicks);
            }
        }
    }

    public void resize() {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        this.modelRenderTarget.resize(this._getWidth() * (int)guiScale, this._getHeight() * (int)guiScale, true);
    }

    public void onDestroy() {
        this.modelRenderTarget.destroyBuffers();
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementInput) {
        narrationElementInput.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }

    @OnlyIn(Dist.CLIENT)
    public class FakeEntityPatch extends LivingEntityPatch<LivingEntity> {
        public FakeEntityPatch(Armature armature) {
            this.armature = armature.deepCopy();
        }

        public void setAnimator() {
            this.animator = TexturedModelPreviewer.this.animator;
        }

        @Override
        public void initAnimator(Animator clientAnimator) {

        }

        @Override
        public void updateMotion(boolean considerInaction) {

        }

        @Override
        public StaticAnimation getHitAnimation(StunType stunType) {
            return null;
        }

        @Override
        public boolean isLogicalClient() {
            return true;
        }

        @Override
        public void cancelAnyAction() {
        }

        @Override
        public float getAttackDirectionPitch() {
            return 0.0F;
        }

        @Override
        public OpenMatrix4f getModelMatrix(float partialTicks) {
            return MathUtils.getModelMatrixIntegral(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, partialTicks, 1.0F, 1.0F, 1.0F);
        }

        @Override
        public void poseTick(DynamicAnimation animation, Pose pose, float time, float partialTicks) {
        }

        @Override
        public void updateEntityState() {
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class NoEntityAnimator extends ClientAnimator {
        public NoEntityAnimator(FakeEntityPatch entitypatch) {
            super(entitypatch, NoEntityBaseLayer::new);
        }

        @Override
        public void tick() {
            this.baseLayer.update(this.entitypatch);

            if (this.baseLayer.animationPlayer.isEnd() && this.baseLayer.getNextAnimation() == null) {
                StaticAnimation toPlay = TexturedModelPreviewer.this.index > -1 && TexturedModelPreviewer.this.index < TexturedModelPreviewer.this.animationsToPlay.size() ? TexturedModelPreviewer.this.animationsToPlay.get(TexturedModelPreviewer.this.index) : Animations.DUMMY_ANIMATION;
                this.baseLayer.playAnimation(toPlay, this.entitypatch, 0.0F);

                if (!TexturedModelPreviewer.this.trailInfoList.isEmpty()) {
                    for (TrailInfo trailInfo : TexturedModelPreviewer.this.trailInfoList) {
                        if (trailInfo.playable()) {
                            CustomTrailParticle trail = new CustomTrailParticle(TexturedModelPreviewer.this.getArmature().searchJointByName(trailInfo.joint), toPlay, trailInfo);
                            TexturedModelPreviewer.this.trailParticles.add(trail);
                        } else {
                            toPlay.getProperty(ClientAnimationProperties.TRAIL_EFFECT).ifPresent(trailInfos -> {
                                for (TrailInfo info : trailInfos) {
                                    if (info.hand != InteractionHand.MAIN_HAND) {
                                        continue;
                                    }

                                    TrailInfo combinedTrailInfo = trailInfo.overwrite(info);

                                    if (combinedTrailInfo.playable()) {
                                        CustomTrailParticle trail = new CustomTrailParticle(TexturedModelPreviewer.this.getArmature().searchJointByName(combinedTrailInfo.joint), toPlay, combinedTrailInfo);
                                        TexturedModelPreviewer.this.trailParticles.add(trail);
                                    }
                                }
                            });
                        }
                    }
                }

                TexturedModelPreviewer.this.index = (TexturedModelPreviewer.this.index + 1) % TexturedModelPreviewer.this.animationsToPlay.size();
            }
        }

        public LivingEntityPatch<?> getEntityPatch() {
            return this.entitypatch;
        }

        @OnlyIn(Dist.CLIENT)
        static class NoEntityAnimationPlayer extends AnimationPlayer {
            @Override
            public void tick(LivingEntityPatch<?> entitypatch) {
                this.prevElapsedTime = this.elapsedTime;
                this.elapsedTime += EpicFightOptions.A_TICK * (this.isReversed() && this.getAnimation().canBePlayedReverse() ? -1.0F : 1.0F);

                if (this.elapsedTime >= this.play.getTotalTime()) {
                    if (this.play.isRepeat()) {
                        this.prevElapsedTime = 0;
                        this.elapsedTime %= this.play.getTotalTime();
                    } else {
                        this.elapsedTime = this.play.getTotalTime();
                        this.isEnd = true;
                    }
                } else if (this.elapsedTime < 0) {
                    if (this.play.isRepeat()) {
                        this.prevElapsedTime = this.play.getTotalTime();
                        this.elapsedTime = this.play.getTotalTime() + this.elapsedTime;
                    } else {
                        this.elapsedTime = 0.0F;
                        this.isEnd = true;
                    }
                }
            }

            @Override
            public void begin(DynamicAnimation animation, LivingEntityPatch<?> entitypatch) {
            }

            @Override
            public Pose getCurrentPose(LivingEntityPatch<?> entitypatch, float partialTicks) {
                return this.play.getRawPose(this.prevElapsedTime + (this.elapsedTime - this.prevElapsedTime) * partialTicks);
            }
        }

        @OnlyIn(Dist.CLIENT)
        static class NoEntityLayer extends Layer {
            public NoEntityLayer(Priority priority) {
                super(priority, NoEntityAnimationPlayer::new);
            }

            public void playAnimation(StaticAnimation nextAnimation, LivingEntityPatch<?> entitypatch, float convertTimeModifier) {
                Pose lastPose = entitypatch.getAnimator().getPose(1.0F);
                this.resume();

                if (!nextAnimation.isMetaAnimation()) {
                    this.setLinkAnimation(nextAnimation, entitypatch, lastPose, convertTimeModifier);
                    this.linkAnimation.putOnPlayer(this.animationPlayer, entitypatch);
                    this.nextAnimation = nextAnimation;
                }
            }

            public void playAnimationInstant(DynamicAnimation nextAnimation, LivingEntityPatch<?> entitypatch) {
                this.resume();
                nextAnimation.putOnPlayer(this.animationPlayer, entitypatch);
                this.nextAnimation = null;
            }

            @Override
            protected void setLinkAnimation(StaticAnimation nextAnimation, LivingEntityPatch<?> entitypatch, Pose lastPose, float convertTimeModifier) {
                Pose currentPose = this.animationPlayer.getAnimation().getRawPose(this.animationPlayer.getElapsedTime());
                Pose nextAnimationPose = nextAnimation.getRawPose(0.0F);
                float totalTime = nextAnimation.getConvertTime();

                DynamicAnimation fromAnimation = this.animationPlayer.isEmpty() ? entitypatch.getClientAnimator().baseLayer.animationPlayer.getAnimation() : this.animationPlayer.getAnimation();

                if (fromAnimation instanceof LinkAnimation linkAnimation) {
                    fromAnimation = linkAnimation.getFromAnimation();
                }

                this.linkAnimation.getTransfroms().clear();
                this.linkAnimation.setTotalTime(totalTime);
                this.linkAnimation.setConnectedAnimations(fromAnimation, nextAnimation);

                Map<String, JointTransform> data1 = currentPose.getJointTransformData();
                Map<String, JointTransform> data2 = nextAnimationPose.getJointTransformData();

                for (String jointName : data1.keySet()) {
                    if (data1.containsKey(jointName) && data2.containsKey(jointName)) {
                        Keyframe[] keyframes = new Keyframe[2];
                        keyframes[0] = new Keyframe(0.0F, data1.get(jointName));
                        keyframes[1] = new Keyframe(totalTime, data2.get(jointName));
                        TransformSheet sheet = new TransformSheet(keyframes);
                        this.linkAnimation.getAnimationClip().addJointTransform(jointName, sheet);
                    }
                }

                this.animationPlayer.setPlayAnimation(this.linkAnimation);
            }

            public void update(LivingEntityPatch<?> entitypatch) {
                if (this.paused) {
                    this.animationPlayer.setElapsedTime(this.animationPlayer.getElapsedTime());
                } else {
                    this.animationPlayer.tick(entitypatch);
                }

                if (!this.paused && this.animationPlayer.isEnd()) {
                    if (this.nextAnimation != null) {
                        this.nextAnimation.putOnPlayer(this.animationPlayer, entitypatch);
                        this.nextAnimation = null;
                    } else {
                        if (this.animationPlayer.getAnimation() instanceof LayerOffAnimation) {
                            this.animationPlayer.getAnimation().end(entitypatch, Animations.DUMMY_ANIMATION, true);
                        } else {
                            this.off(entitypatch);
                        }
                    }
                }
            }

            public Pose getEnabledPose(LivingEntityPatch<?> entitypatch, float partialTick) {
                DynamicAnimation animation = this.animationPlayer.getAnimation();
                Pose pose = animation.getRawPose(this.animationPlayer.getPrevElapsedTime() + (this.animationPlayer.getElapsedTime() - this.animationPlayer.getPrevElapsedTime()) * partialTick);
                pose.removeJointIf((entry) -> !animation.hasTransformFor(entry.getKey()));

                return pose;
            }

            public void off(LivingEntityPatch<?> entitypatch) {
                if (!this.isDisabled() && !(this.animationPlayer.getAnimation() instanceof LayerOffAnimation)) {
                    float convertTime = entitypatch.getClientAnimator().baseLayer.animationPlayer.getAnimation().getConvertTime();
                    setLayerOffAnimation(this.animationPlayer.getAnimation(), this.getEnabledPose(entitypatch, 1.0F), this.layerOffAnimation, convertTime);
                    this.playAnimationInstant(this.layerOffAnimation, entitypatch);
                }
            }
        }

        @OnlyIn(Dist.CLIENT)
        static class NoEntityBaseLayer extends Layer.BaseLayer {
            public NoEntityBaseLayer() {
                super(NoEntityAnimationPlayer::new);

                this.compositeLayers.clear();
                this.compositeLayers.computeIfAbsent(Priority.LOWEST, NoEntityLayer::new);
                this.compositeLayers.computeIfAbsent(Priority.MIDDLE, NoEntityLayer::new);
                this.compositeLayers.computeIfAbsent(Priority.HIGHEST, NoEntityLayer::new);
            }

            @Override
            public void playAnimation(StaticAnimation nextAnimation, LivingEntityPatch<?> entitypatch, float convertTimeModifier) {
                this.baseLayerPriority = nextAnimation.getPriority();
                this.offCompositeLayerLowerThan(entitypatch, nextAnimation);

                Pose lastPose = entitypatch.getAnimator().getPose(1.0F);
                this.resume();

                if (!nextAnimation.isMetaAnimation()) {
                    this.setLinkAnimation(nextAnimation, entitypatch, lastPose, convertTimeModifier);
                    this.linkAnimation.putOnPlayer(this.animationPlayer, entitypatch);
                    entitypatch.updateEntityState();
                    this.nextAnimation = nextAnimation;
                }
            }

            @Override
            public void playAnimationInstant(DynamicAnimation nextAnimation, LivingEntityPatch<?> entitypatch) {
                this.resume();
                nextAnimation.putOnPlayer(this.animationPlayer, entitypatch);
                this.nextAnimation = null;
            }

            @Override
            protected void playLivingAnimation(StaticAnimation nextAnimation, LivingEntityPatch<?> entitypatch) {
                this.resume();

                if (!nextAnimation.isMetaAnimation()) {
                    this.concurrentLinkAnimation.acceptFrom(this.animationPlayer.getAnimation().getRealAnimation(), nextAnimation, this.animationPlayer.getElapsedTime());
                    this.concurrentLinkAnimation.putOnPlayer(this.animationPlayer, entitypatch);
                    this.nextAnimation = nextAnimation;
                }
            }

            @Override
            public void update(LivingEntityPatch<?> entitypatch) {
                if (this.paused) {
                    this.animationPlayer.setElapsedTime(this.animationPlayer.getElapsedTime());
                } else {
                    this.animationPlayer.tick(entitypatch);
                }

                if (!this.paused && this.animationPlayer.isEnd()) {
                    if (this.nextAnimation != null) {
                        this.nextAnimation.putOnPlayer(this.animationPlayer, entitypatch);
                        this.nextAnimation = null;
                    } else {
                        if (this.animationPlayer.getAnimation() instanceof LayerOffAnimation) {
                            this.animationPlayer.getAnimation().end(entitypatch, Animations.DUMMY_ANIMATION, true);
                        } else {
                            this.off(entitypatch);
                        }
                    }
                }

                for (Layer layer : this.compositeLayers.values()) {
                    layer.update(entitypatch);
                }
            }

            @Override
            protected void setLinkAnimation(StaticAnimation nextAnimation, LivingEntityPatch<?> entitypatch, Pose lastPose, float convertTimeModifier) {
                Pose currentPose = this.animationPlayer.getAnimation().getRawPose(this.animationPlayer.getElapsedTime());
                Pose nextAnimationPose = nextAnimation.getRawPose(0.0F);
                float totalTime = nextAnimation.getConvertTime();

                DynamicAnimation fromAnimation = this.animationPlayer.isEmpty() ? entitypatch.getClientAnimator().baseLayer.animationPlayer.getAnimation() : this.animationPlayer.getAnimation();

                if (fromAnimation instanceof LinkAnimation linkAnimation) {
                    fromAnimation = linkAnimation.getFromAnimation();
                }

                this.linkAnimation.getTransfroms().clear();
                this.linkAnimation.setTotalTime(totalTime);
                this.linkAnimation.setConnectedAnimations(fromAnimation, nextAnimation);

                Map<String, JointTransform> data1 = currentPose.getJointTransformData();
                Map<String, JointTransform> data2 = nextAnimationPose.getJointTransformData();

                for (String jointName : data1.keySet()) {
                    if (data1.containsKey(jointName) && data2.containsKey(jointName)) {
                        Keyframe[] keyframes = new Keyframe[2];
                        keyframes[0] = new Keyframe(0.0F, data1.get(jointName));
                        keyframes[1] = new Keyframe(totalTime, data2.get(jointName));
                        TransformSheet sheet = new TransformSheet(keyframes);
                        this.linkAnimation.getAnimationClip().addJointTransform(jointName, sheet);
                    }
                }

                this.animationPlayer.setPlayAnimation(this.linkAnimation);
            }

            public void offCompositeLayerLowerThan(LivingEntityPatch<?> entitypatch, StaticAnimation nextAnimation) {
                for (Priority p : nextAnimation.getPriority().lowerEquals()) {
                    if (p == Priority.LOWEST && !nextAnimation.isMainFrameAnimation()) {
                        continue;
                    }

                    this.compositeLayers.get(p).off(entitypatch);
                }
            }

            public Layer getLayer(Priority priority) {
                return this.compositeLayers.get(priority);
            }

            @Override
            public void off(LivingEntityPatch<?> entitypatch) {
            }

            @Override
            protected boolean isDisabled() {
                return false;
            }

            @Override
            protected boolean isBaseLayer() {
                return true;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    class ModelRenderTarget extends RenderTarget {
        public ModelRenderTarget() {
            super(true);

            RenderSystem.assertOnRenderThreadOrInit();
            Window window = Minecraft.getInstance().getWindow();

            this.resize(window.getWidth(), window.getHeight(), false);
        }

        private void blitToScreen(PoseStack guiGraphics) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, this.colorTextureId);

            float left = TexturedModelPreviewer.this._getX();
            float top = TexturedModelPreviewer.this._getY();
            float right = left + TexturedModelPreviewer.this._getWidth();
            float bottom = top + TexturedModelPreviewer.this._getHeight();

            float u = (float) this.viewWidth / (float) this.width;
            float v = (float) this.viewHeight / (float) this.height;

            guiGraphics.pushPose();

            Matrix4f matrix4f = guiGraphics.last().pose();

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tesselator.getBuilder();
            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            bufferbuilder.vertex(matrix4f, left, bottom, 0.0F).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
            bufferbuilder.vertex(matrix4f, right, bottom, 0.0F).uv(u, 0.0F).color(255, 255, 255, 255).endVertex();
            bufferbuilder.vertex(matrix4f, right, top, 0.0F).uv(u, v).color(255, 255, 255, 255).endVertex();
            bufferbuilder.vertex(matrix4f, left, top, 0.0F).uv(0.0F, v).color(255, 255, 255, 255).endVertex();
            BufferUploader.end(bufferbuilder);
            guiGraphics.popPose();
        }
    }

    @OnlyIn(Dist.CLIENT)
    class CustomTrailParticle extends TrailParticle {
        @SuppressWarnings("deprecation")
        protected CustomTrailParticle(Joint joint, StaticAnimation animation, TrailInfo trailInfo) {
            super(TexturedModelPreviewer.this.getArmature(), TexturedModelPreviewer.this.animator.getEntityPatch(), joint, animation, trailInfo);
        }

        @Override
        public void tick() {
            AnimationPlayer animPlayer = TexturedModelPreviewer.this.animator.getPlayerFor(null);
            this.visibleTrailEdges.removeIf(v -> !v.isAlive());

            if (this.animationEnd) {
                if (this.lifetime-- == 0) {
                    this.remove();
                }
            } else {
                if (this.animation != animPlayer.getAnimation().getRealAnimation() || animPlayer.getElapsedTime() > this.trailInfo.endTime) {
                    this.animationEnd = true;
                    this.lifetime = this.trailInfo.trailLifetime;
                }
            }

            if (this.trailInfo.fadeTime > 0.0F && this.trailInfo.endTime < animPlayer.getElapsedTime()) {
                return;
            }

            boolean isTrailInvisible = animPlayer.getAnimation().isLinkAnimation() || animPlayer.getElapsedTime() <= this.trailInfo.startTime;
            boolean isFirstTrail = this.visibleTrailEdges.isEmpty();
            boolean needCorrection = (!isTrailInvisible && isFirstTrail);

            if (needCorrection) {
                float startCorrection = Math.max((this.trailInfo.startTime - animPlayer.getPrevElapsedTime()) / (animPlayer.getElapsedTime() - animPlayer.getPrevElapsedTime()), 0.0F);
                this.startEdgeCorrection = this.trailInfo.interpolateCount * 2 * startCorrection;
            }

            TrailInfo trailInfo = this.trailInfo;
            Pose prevPose = this.entitypatch.getAnimator().getPose(0.0F);
            Pose middlePose = this.entitypatch.getAnimator().getPose(0.5F);
            Pose currentPose = this.entitypatch.getAnimator().getPose(1.0F);
            OpenMatrix4f prevJointTf = TexturedModelPreviewer.this.getArmature().getBindedTransformFor(prevPose, this.joint);
            OpenMatrix4f middleJointTf = TexturedModelPreviewer.this.getArmature().getBindedTransformFor(middlePose, this.joint);
            OpenMatrix4f currentJointTf = TexturedModelPreviewer.this.getArmature().getBindedTransformFor(currentPose, this.joint);
            Vec3 prevStartPos = OpenMatrix4f.transform(prevJointTf, trailInfo.start);
            Vec3 prevEndPos = OpenMatrix4f.transform(prevJointTf, trailInfo.end);
            Vec3 middleStartPos = OpenMatrix4f.transform(middleJointTf, trailInfo.start);
            Vec3 middleEndPos = OpenMatrix4f.transform(middleJointTf, trailInfo.end);
            Vec3 currentStartPos = OpenMatrix4f.transform(currentJointTf, trailInfo.start);
            Vec3 currentEndPos = OpenMatrix4f.transform(currentJointTf, trailInfo.end);

            List<Vec3> finalStartPositions;
            List<Vec3> finalEndPositions;
            boolean visibleTrail;

            if (isTrailInvisible) {
                finalStartPositions = Lists.newArrayList();
                finalEndPositions = Lists.newArrayList();
                finalStartPositions.add(prevStartPos);
                finalStartPositions.add(middleStartPos);
                finalEndPositions.add(prevEndPos);
                finalEndPositions.add(middleEndPos);

                this.invisibleTrailEdges.clear();
                visibleTrail = false;
            } else {
                List<Vec3> startPosList = Lists.newArrayList();
                List<Vec3> endPosList = Lists.newArrayList();
                TrailEdge edge1;
                TrailEdge edge2;

                if (isFirstTrail) {
                    int lastIdx = this.invisibleTrailEdges.size() - 1;
                    edge1 = this.invisibleTrailEdges.get(lastIdx);
                    edge2 = new TrailEdge(prevStartPos, prevEndPos, -1);
                } else {
                    edge1 = this.visibleTrailEdges.get(this.visibleTrailEdges.size() - (this.trailInfo.interpolateCount / 2 + 1));
                    edge2 = this.visibleTrailEdges.get(this.visibleTrailEdges.size() - 1);
                    edge2.lifetime++;
                }

                startPosList.add(edge1.start);
                endPosList.add(edge1.end);
                startPosList.add(edge2.start);
                endPosList.add(edge2.end);
                startPosList.add(middleStartPos);
                endPosList.add(middleEndPos);
                startPosList.add(currentStartPos);
                endPosList.add(currentEndPos);

                finalStartPositions = CubicBezierCurve.getBezierInterpolatedPoints(startPosList, 1, 3, this.trailInfo.interpolateCount);
                finalEndPositions = CubicBezierCurve.getBezierInterpolatedPoints(endPosList, 1, 3, this.trailInfo.interpolateCount);

                if (!isFirstTrail) {
                    finalStartPositions.remove(0);
                    finalEndPositions.remove(0);
                }

                visibleTrail = true;
            }

            this.makeTrailEdges(finalStartPositions, finalEndPositions, visibleTrail ? this.visibleTrailEdges : this.invisibleTrailEdges);
        }

        @Override
        public void render(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
            if (this.visibleTrailEdges.isEmpty()) {
                return;
            }

            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            AbstractTexture abstracttexture = texturemanager.getTexture(this.trailInfo.texturePath);
            RenderSystem.bindTexture(abstracttexture.getId());
            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            RenderSystem.setShaderTexture(0, abstracttexture.getId());

            PoseStack poseStack = new PoseStack();
            this.setupPoseStack(poseStack, camera, partialTick);
            Matrix4f matrix4f = poseStack.last().pose();
            int edges = this.visibleTrailEdges.size() - 1;
            boolean startFade = this.visibleTrailEdges.get(0).lifetime == 1;
            boolean endFade = this.visibleTrailEdges.get(edges).lifetime == this.trailInfo.trailLifetime;
            float startEdge = (startFade ? this.trailInfo.interpolateCount * 2 * partialTick : 0.0F) + this.startEdgeCorrection;
            float endEdge = endFade ? Math.min(edges - (this.trailInfo.interpolateCount * 2) * (1.0F - partialTick), edges - 1) : edges - 1;
            float interval = 1.0F / (endEdge - startEdge);
            float fading = 1.0F;

            if (this.animationEnd) {
                if (TrailInfo.isValidTime(this.trailInfo.fadeTime)) {
                    fading = ((float)this.lifetime / (float)this.trailInfo.trailLifetime);
                } else {
                    fading = Mth.clamp((this.lifetime + (1.0F - partialTick)) / this.trailInfo.trailLifetime, 0.0F, 1.0F);
                }
            }

            float partialStartEdge = interval * (startEdge % 1.0F);
            float from = -partialStartEdge;
            float to = -partialStartEdge + interval;

            for (int i = (int)(startEdge); i < (int)endEdge + 1; i++) {
                TrailEdge e1 = this.visibleTrailEdges.get(i);
                TrailEdge e2 = this.visibleTrailEdges.get(i + 1);
                Vector4f pos1 = new Vector4f((float)e1.start.x, (float)e1.start.y, (float)e1.start.z, 1.0F);
                Vector4f pos2 = new Vector4f((float)e1.end.x, (float)e1.end.y, (float)e1.end.z, 1.0F);
                Vector4f pos3 = new Vector4f((float)e2.end.x, (float)e2.end.y, (float)e2.end.z, 1.0F);
                Vector4f pos4 = new Vector4f((float)e2.start.x, (float)e2.start.y, (float)e2.start.z, 1.0F);

                pos1.mul(matrix4f);
                pos2.mul(matrix4f);
                pos3.mul(matrix4f);
                pos4.mul(matrix4f);

                float alphaFrom = Mth.clamp(from, 0.0F, 1.0F);
                float alphaTo = Mth.clamp(to, 0.0F, 1.0F);

                vertexConsumer.vertex(pos1.x(), pos1.y(), pos1.z()).uv(from, 1.0F).color(this.rCol, this.gCol, this.bCol, this.alpha * alphaFrom * fading).uv2(0).endVertex();
                vertexConsumer.vertex(pos2.x(), pos2.y(), pos2.z()).uv(from, 0.0F).color(this.rCol, this.gCol, this.bCol, this.alpha * alphaFrom * fading).uv2(0).endVertex();
                vertexConsumer.vertex(pos3.x(), pos3.y(), pos3.z()).uv(to, 0.0F).color(this.rCol, this.gCol, this.bCol, this.alpha * alphaTo * fading).uv2(0).endVertex();
                vertexConsumer.vertex(pos4.x(), pos4.y(), pos4.z()).uv(to, 1.0F).color(this.rCol, this.gCol, this.bCol, this.alpha * alphaTo * fading).uv2(0).endVertex();

                from += interval;
                to += interval;
            }
        }

        @Override
        protected void setupPoseStack(PoseStack poseStack, Camera camera, float partialTicks) {
            float x = (float) TexturedModelPreviewer.this.xMove;
            float y = (float) TexturedModelPreviewer.this.yMove;
            float z = (float) TexturedModelPreviewer.this.zoom;
            float xRot = TexturedModelPreviewer.this.xRot;
            float yRot = TexturedModelPreviewer.this.yRot;

            poseStack.translate(x, y - 1.0D, z);
            poseStack.mulPose(QuaternionUtils.XP.rotationDegrees(xRot));
            poseStack.mulPose(QuaternionUtils.YP.rotationDegrees(yRot));
        }
    }

    /*******************************************************************
     * @ResizableComponent variables                                   *
     *******************************************************************/
    private int x1;
    private int x2;
    private int y1;
    private int y2;
    private final HorizontalSizing horizontalSizingOption;
    private final VerticalSizing verticalSizingOption;

    @Override
    public void setX1(int x1) {
        this.x1 = x1;
    }

    @Override
    public void setX2(int x2) {
        this.x2 = x2;
    }

    @Override
    public void setY1(int y1) {
        this.y1 = y1;
    }

    @Override
    public void setY2(int y2) {
        this.y2 = y2;
    }

    @Override
    public int getX1() {
        return this.x1;
    }

    @Override
    public int getX2() {
        return this.x2;
    }

    @Override
    public int getY1() {
        return this.y1;
    }

    @Override
    public int getY2() {
        return this.y2;
    }

    @Override
    public HorizontalSizing getHorizontalSizingOption() {
        return this.horizontalSizingOption;
    }

    @Override
    public VerticalSizing getVerticalSizingOption() {
        return this.verticalSizingOption;
    }

    @Override
    public void _setActive(boolean active) {
    }

    @Override
    public int _getX() {
        return this.x;
    }

    @Override
    public int _getY() {
        return this.y;
    }

    @Override
    public int _getWidth() {
        return this.getWidth();
    }

    @Override
    public int _getHeight() {
        return this.getHeight();
    }

    @Override
    public void _setX(int x) {
        this.x = (x);
    }

    @Override
    public void _setY(int y) {
        this.y = (y);
    }

    @Override
    public void _setWidth(int width) {
        this.setWidth(width);
    }

    @Override
    public void _setHeight(int height) {
        this.setHeight(height);
    }

    @Override
    public Component _getMessage() {
        return this.getMessage();
    }

    public void _render(PoseStack guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
}