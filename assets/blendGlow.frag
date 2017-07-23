#version 120


varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture; // world texture, has alpha value that is meaningful

uniform sampler2D tex1; // glow texture, SHOULD contain alpha of all 1.0

void main(void) {
    vec4 colorTex0 = texture2D(u_texture, v_texCoords); // lightmap (RGB) pre-mixed
    vec4 colorTex1 = texture2D(tex1, v_texCoords); // lightmap (A) pre-mixed

    vec4 newColor = vec4(0.0, 0.0, 0.0, colorTex0.a);


    if (colorTex0.r > colorTex1.r) newColor.r = colorTex0.r;
    else                           newColor.r = colorTex1.r;

    if (colorTex0.g > colorTex1.g) newColor.g = colorTex0.g;
    else                           newColor.g = colorTex1.g;

    if (colorTex0.b > colorTex1.b) newColor.b = colorTex0.b;
    else                           newColor.b = colorTex1.b;


    gl_FragColor = newColor;
}