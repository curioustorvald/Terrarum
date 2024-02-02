#ifdef GL_ES
    precision mediump float;
#endif


in vec4 v_color;
in vec2 v_texCoords;
uniform sampler2D u_texture;

uniform vec2 resolution;

uniform vec3 phosphor_colour = vec3(1.3, 0.8567, 0.0);
vec3 scanline_darkening = vec3(0.66, 0.66, 0.66);

// 0: every odd line will get darkened; 1: every even line will get darkened
uniform float alternative_scanline = 0.0; // 1.0: true

uniform float blur_blend = 0.5;
out vec4 fragColor;

void main(void) {
    vec4 color = texture(u_texture, v_texCoords).rgba;
    vec4 color_pre =  texture(u_texture, (v_texCoords + (vec2(-1.0, 0.0) / resolution))).rgba;
    vec4 color_next = texture(u_texture, (v_texCoords + (vec2( 1.0, 0.0) / resolution))).rgba;

    color = color * (1.0 - blur_blend) + color_pre * (blur_blend / 2.0) + color_next * (blur_blend / 2.0);

    bool is_scanline = mod(int(gl_FragCoord.y), 2) == int(alternative_scanline);

    float color_luminosity = (
        3.0 * color.r +
        4.0 * color.g +
        1.0 * color.b
    ) / 8.0;

    // out colour
    vec3 out_color = vec3(color_luminosity) * phosphor_colour;

    if (is_scanline) {
        out_color = out_color * scanline_darkening;
    }

    fragColor = vec4(out_color, color.a);
    //fragColor = texture(u_texture, v_texCoords);
}