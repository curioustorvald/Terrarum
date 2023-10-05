/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

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
 * This version of code is based on GDX 1.12.0 and was modified for the Terrarum project.
 *
 * Created by minjaesong on 2023-08-16.
 */
public class Float16FrameBuffer extends FrameBuffer {

    private static boolean floatFramebufferAvailable = !App.operationSystem.equals("OSX");

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
        if (!floatFramebufferAvailable) {
            FrameBufferBuilder bufferBuilder = new FrameBufferBuilder(width, height);
            bufferBuilder.addColorTextureAttachment(GL20.GL_RGBA, GL20.GL_RGBA, GL20.GL_UNSIGNED_SHORT); // but 16bpp creates slight banding
            if (hasDepth) bufferBuilder.addBasicDepthRenderBuffer();
            this.bufferBuilder = bufferBuilder;
            build();
        }
        else {
            try {
                FloatFrameBufferBuilder bufferBuilder = new FloatFrameBufferBuilder(width, height);
                bufferBuilder.addFloatAttachment(GL30.GL_RGBA16F, GL30.GL_RGBA, GL30.GL_FLOAT, false); // float16 will not create a banding
                if (hasDepth) bufferBuilder.addBasicDepthRenderBuffer();
                this.bufferBuilder = bufferBuilder;
                build();
            }
            catch (GdxRuntimeException e) {
                floatFramebufferAvailable = false;
                System.out.println("[Float16FrameBuffer] WARNING: Float Framebuffer is not available, falling back to 16bpp...");
                FrameBufferBuilder bufferBuilder = new FrameBufferBuilder(width, height);
                bufferBuilder.addColorTextureAttachment(GL20.GL_RGBA, GL20.GL_RGBA, GL20.GL_UNSIGNED_SHORT); // but 16bpp creates slight banding
                if (hasDepth) bufferBuilder.addBasicDepthRenderBuffer();
                this.bufferBuilder = bufferBuilder;
                build();
            }
        }
    }

    @Override
    protected Texture createTexture (FrameBufferTextureAttachmentSpec attachmentSpec) {
        if (!floatFramebufferAvailable) {
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
        }
    }

}
