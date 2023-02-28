#version 150
#ifdef GL_ES
    precision mediump float;
#endif

in vec4 v_color;
in vec2 v_texCoords;

uniform sampler2D u_texture; // world texture, has alpha value that is meaningful

uniform sampler2D tex1; // lightmap texture
uniform vec2 tex1Offset;
uniform vec2 tex1Resolution;

out vec4 fragColor;

void main() {
	vec4 colorTex0 = texture(u_texture, v_texCoords + vec2(0.5, 0.5)); // world texture
    vec4 colorTex1 = texture(tex1, v_texCoords); // lightmap (RGBA)

    colorTex1 = vec4(colorTex1.xyz, 1.0);

    fragColor = colorTex0 * colorTex1;
}
