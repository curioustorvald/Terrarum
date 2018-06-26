#version 120
#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture; // world texture, has alpha value that is meaningful

uniform sampler2D tex1; // lightmap texture

void main() {
	vec4 colorTex0 = texture2D(u_texture, v_texCoords); // world texture
    vec4 colorTex1 = texture2D(tex1, v_texCoords); // lightmap (RGBA)

    gl_FragColor = colorTex0 * vec4(colorTex1.rgb, 1.0);
}
