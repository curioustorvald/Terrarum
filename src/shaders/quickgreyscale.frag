#version 150
#ifdef GL_ES
    precision mediump float;
#endif

in vec4 v_color;
in vec2 v_texCoords;

uniform sampler2D u_texture;
uniform mat4 u_projTrans;

out vec4 fragColor;

void main() {
    vec3 color = texture(u_texture, v_texCoords).rgb;
    float gray = (3.0 * color.r + 4.0 * color.g + color.b) / 8.0;
    vec3 grayscale = vec3(gray);

    fragColor = vec4(grayscale, 1.0);
}