package com.runal.client;

//? if 1.21.4 || 1.21.11 || 26.2 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;

// NanoVG picture-in-picture rendering needs Minecraft's PictureInPictureRenderState/
// GuiRenderState render-graph. On 1.21.4/1.21.11 that pipeline doesn't exist at all; on
// 26.2 it exists but its MultiBufferSource/PictureInPictureRenderer wiring has moved
// (see RunalScreen.renderContentNVG's 26.2 short-circuit to the legacy renderer) and
// hasn't been ported yet. RunalScreen never calls draw() here on these versions, so
// this stub only needs to exist to satisfy the compiler.
public class NVGPIPRenderer {
    public static void draw(GuiGraphicsExtractor context, int x, int y, int width, int height, Runnable renderContent) {
    }
}
*///?} else {
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.lwjgl.opengl.GL33C;

/**
 * Picture-in-Picture renderer that lets NVGRenderer draw into the vanilla GUI pipeline.
 * Ported from Melinoe's me.melinoe.utils.ui.rendering.NVGPIPRenderer.
 */
public class NVGPIPRenderer extends PictureInPictureRenderer<NVGPIPRenderer.NVGRenderState> {

    public NVGPIPRenderer(MultiBufferSource.BufferSource vertexConsumers) {
        super(vertexConsumers);
    }

    @Override
    protected void renderToTexture(NVGRenderState state, PoseStack poseStack) {
        var colorTex = RenderSystem.outputColorTextureOverride;
        if (colorTex == null) return;
        if (!(RenderSystem.getDevice().backend instanceof GlDevice glDevice)) return;
        var directStateAccess = glDevice.directStateAccess();
        var depthOverride = RenderSystem.outputDepthTextureOverride;
        if (depthOverride == null || !(depthOverride.texture() instanceof GlTexture glDepthTex)) return;

        int width = colorTex.getWidth(0);
        int height = colorTex.getHeight(0);
        if (colorTex.texture() instanceof GlTexture glColorTex) {
            int fbo = glColorTex.getFbo(directStateAccess, glDepthTex);
            GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, fbo);
            GlStateManager._viewport(0, 0, width, height);
        }

        GL33C.glBindSampler(0, 0);
        NVGRenderer.beginFrame(width, height);
        state.renderContent.run();
        NVGRenderer.endFrame();

        // Restore typical Minecraft GL state that NVG might have disrupted
        GlStateManager._disableDepthTest();
        GlStateManager._disableCull();
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(770, 771, 1, 0);
    }

    @Override
    protected float getTranslateY(int height, int windowScaleFactor) {
        return height / 2f;
    }

    @Override
    public Class<NVGRenderState> getRenderStateClass() {
        return NVGRenderState.class;
    }

    @Override
    public String getTextureLabel() {
        return "nvg_renderer";
    }

    public record NVGRenderState(
            int x, int y, int width, int height,
            Matrix3x2f poseMatrix,
            ScreenRectangle scissor,
            ScreenRectangle bounds,
            Runnable renderContent
    ) implements PictureInPictureRenderState {
        @Override
        public float scale() {
            return 1f;
        }

        @Override
        public int x0() {
            return x;
        }

        @Override
        public int y0() {
            return y;
        }

        @Override
        public int x1() {
            return x + width;
        }

        @Override
        public int y1() {
            return y + height;
        }

        @Override
        public ScreenRectangle scissorArea() {
            return scissor;
        }

        @Override
        public ScreenRectangle bounds() {
            return bounds;
        }
    }

    /**
     * Draw NVG content as a special GUI element.
     *
     * @param context       the GuiGraphicsExtractor to draw to
     * @param x             the x position
     * @param y             the y position
     * @param width         the width of the rendering area
     * @param height        the height of the rendering area
     * @param renderContent a callback that draws the NVG content
     */
    public static void draw(GuiGraphicsExtractor context, int x, int y, int width, int height, Runnable renderContent) {
        // The PiP texture is sized from this box, so an empty or inverted one would crash
        if (width <= 0 || height <= 0) return;
        ScreenRectangle scissor = context.scissorStack.peek();
        Matrix3x2f pose = new Matrix3x2f(context.pose());
        ScreenRectangle bounds = createBounds(x, y, x + width, y + height, pose, scissor);

        NVGRenderState state = new NVGRenderState(x, y, width, height, pose, scissor, bounds, renderContent);
        context.guiRenderState.addPicturesInPictureState(state);
    }

    private static ScreenRectangle createBounds(int x0, int y0, int x1, int y1, Matrix3x2f pose, ScreenRectangle scissorArea) {
        ScreenRectangle screenRect = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(screenRect) : screenRect;
    }
}
//?}
