#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

varying vec4 vertexWorldPos;
uniform vec4 backColor;

uniform float width;
uniform float height;
void main() {
    vec2 tex;
    vec4 color2=vec4(0.0,0.0,0.0,0);

    float stepx=(1.0/width)*4.0;
    float stepy=(1.0/height)*4.0;
    vec4 color;

    tex.x=v_texCoords.x+stepx;
    tex.y=v_texCoords.y+stepy;
    color=v_color * texture2D(u_texture, tex);
    color2.r+=color.r;
    color2.g+=color.g;
    color2.b+=color.b;
    color2.a+=color.a;

    tex.x=v_texCoords.x-stepx;
    tex.y=v_texCoords.y+stepy;
    color=v_color * texture2D(u_texture, tex);
    color2.r+=color.r;
    color2.g+=color.g;
    color2.b+=color.b;
    color2.a+=color.a;

    tex.x=v_texCoords.x-stepx;
    tex.y=v_texCoords.y-stepy;
    color=v_color * texture2D(u_texture, tex);
    color2.r+=color.r;
    color2.g+=color.g;
    color2.b+=color.b;
    color2.a+=color.a;

    tex.x=v_texCoords.x+stepx;
    tex.y=v_texCoords.y-stepy;
    color=v_color * texture2D(u_texture, tex);
    color2.r+=color.r;
    color2.g+=color.g;
    color2.b+=color.b;
    color2.a+=color.a;

    tex.x=v_texCoords.x;
    tex.y=v_texCoords.y;
    color=v_color * texture2D(u_texture, tex);
    color2.r+=color.r;
    color2.g+=color.g;
    color2.b+=color.b;
    color2.a+=color.a;

    color2.r/=5.0;
    color2.g/=5.0;
    color2.b/=5.0;
    color2.a/=5.0;


    gl_FragColor = color2;

}