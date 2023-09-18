package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.GdxRuntimeException;
import net.torvald.terrarum.App;

// typealias Float16FrameBuffer = FloatFrameBuffer

/**
 * Created by minjaesong on 2023-08-16.
 */
public class Float16FrameBuffer extends FrameBuffer {

    Float16FrameBuffer () {
    }

    /** Creates a GLFrameBuffer from the specifications provided by bufferBuilder
     *
     * @param bufferBuilder **/
    protected Float16FrameBuffer (GLFrameBufferBuilder<? extends GLFrameBuffer<Texture>> bufferBuilder) {
        super(bufferBuilder);
    }

    /** Creates a new FrameBuffer with a float backing texture, having the given dimensions and potentially a depth buffer
     * attached.
     *
     * @param width the width of the framebuffer in pixels
     * @param height the height of the framebuffer in pixels
     * @param hasDepth whether to attach a depth buffer
     * @throws GdxRuntimeException in case the FrameBuffer could not be created */
    public Float16FrameBuffer (int width, int height, boolean hasDepth) {
        /*if (!App.gl40capable || App.operationSystem.equals("OSX")) { // disable float framebuffer for Apple M chips
            FrameBufferBuilder bufferBuilder = new FrameBufferBuilder(width, height);
            bufferBuilder.addColorTextureAttachment(GL20.GL_RGBA, GL20.GL_RGBA, GL20.GL_UNSIGNED_SHORT); // but 16bpp int works perfectly?!
            if (hasDepth) bufferBuilder.addBasicDepthRenderBuffer();
            this.bufferBuilder = bufferBuilder;
        }
        else {
            FloatFrameBufferBuilder bufferBuilder = new FloatFrameBufferBuilder(width, height);
            bufferBuilder.addFloatAttachment(GL30.GL_RGBA16F, GL30.GL_RGBA, GL30.GL_FLOAT, false);
            if (hasDepth) bufferBuilder.addBasicDepthRenderBuffer();
            this.bufferBuilder = bufferBuilder;
        }*/

        FrameBufferBuilder bufferBuilder = new FrameBufferBuilder(width, height);
        bufferBuilder.addColorTextureAttachment(GL20.GL_RGBA, GL20.GL_RGBA, GL20.GL_UNSIGNED_SHORT); // but 16bpp int works perfectly?!
        if (hasDepth) bufferBuilder.addBasicDepthRenderBuffer();
        this.bufferBuilder = bufferBuilder;

        build();
    }

    @Override
    protected Texture createTexture (FrameBufferTextureAttachmentSpec attachmentSpec) {
        /*if (!App.gl40capable || App.operationSystem.equals("OSX")) {
            GLOnlyTextureData data = new GLOnlyTextureData(bufferBuilder.width, bufferBuilder.height, 0, attachmentSpec.internalFormat,
                    attachmentSpec.format, attachmentSpec.type);
            Texture result = new Texture(data);
            result.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            result.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
            return result;
        }
        else {
            FloatTextureData data = new FloatTextureData(bufferBuilder.width, bufferBuilder.height, attachmentSpec.internalFormat,
                    attachmentSpec.format, attachmentSpec.type, attachmentSpec.isGpuOnly);
            Texture result = new Texture(data);
            result.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            result.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
            return result;
        }*/

        GLOnlyTextureData data = new GLOnlyTextureData(bufferBuilder.width, bufferBuilder.height, 0, attachmentSpec.internalFormat,
                attachmentSpec.format, attachmentSpec.type);
        Texture result = new Texture(data);
        result.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        result.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return result;
    }

}
