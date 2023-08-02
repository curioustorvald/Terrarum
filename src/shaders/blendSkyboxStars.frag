#version 150
#ifdef GL_ES
    precision mediump float;
#endif


in vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture; // world texture, has alpha value that is meaningful

uniform sampler2D tex1; // glow texture, SHOULD contain alpha of all 1.0
out vec4 fragColor;

vec2 boolean = vec2(0.0, 1.0);

uniform vec2 screenSize;
uniform vec2 drawOffset; // value of the 'gradY'
uniform vec2 drawOffsetSize; // value of the 'gradH'
uniform vec2 skyboxUV1; // (u, v) for the skybox drawing
uniform vec2 skyboxUV2; // (u2, v2) for the skybox drawing
uniform vec2 tex1Size = vec2(4096.0, 4096.0);
uniform vec2 astrumScroll = vec2(0.0, 0.0);


// draw call to this function must use UV coord of (0,0,1,1)!
void main(void) {
    vec2 skyboxTexCoord = mix(skyboxUV1, skyboxUV2, v_texCoords);
    vec2 astrumTexCoord = (v_texCoords * drawOffsetSize + drawOffset + astrumScroll) / tex1Size;


    vec4 colorTex0 = texture(u_texture, skyboxTexCoord); // lightmap (RGB) pre-mixed
    vec4 colorTex1 = texture(tex1, astrumTexCoord); // lightmap (A) pre-mixed



//    fragColor = (max(colorTex0, colorTex1) * boolean.yyyx) + boolean.xxxy;
    fragColor = colorTex0;
}