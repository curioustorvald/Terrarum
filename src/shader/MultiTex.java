package shader;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.newdawn.slick.Color;
import org.newdawn.slick.Renderable;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.opengl.InternalTextureLoader;
import org.newdawn.slick.opengl.Texture;

/**
 * Class to support the concept of a single artifact being
 * comprised of multiple image resources.</br>
 * For example a colourmap, normalmap, diffusemap, and specularmap.
 * This is currently extremely buggy, and I don't know why some
 * things have to be the way the are. 
 * @author Chronocide (Jeremy Klix)
 *
 *
 * All drawing is done starting from the top left vertex and
 * moving counter clockwise.</br>
 */
//TODO Make interface feel a little more like the familiar Image class
//TODO Determine a method of dealing with the case were textures
//are not all the same size.  For instance should textures be
//stretched, tiled, clamped?
//TODO Way of handling images larger then the supporting cards
//max texture size ala Slicks BigImage class.
//TODO Needs way more attention to documenting inheritance.
//TODO Test using different numbers of textures.
public class MultiTex implements Renderable{
  private static int units = -1; 
  
  /** The top left corner identifier */
  public static final int TOP_LEFT = 0;
  /** The top right corner identifier */
  public static final int BOTTOM_LEFT = 1;
  /** The bottom left corner identifier */
  public static final int BOTTOM_RIGHT = 3;
  /** The bottom right corner identifier */
  public static final int TOP_RIGHT = 2;
  
  
  private List<Texture> textures;
  
  private int primaryTextureIndex = 0;
  //Width and height based off primary texture loaded
  private float imgWidth, imgHeight;
  //Primary texture width and height clamped between 0 and 1 
  private float texWidth, texHeight; 
  
  
  private float[] normals = new float[]{0,0,1,
                                        0,0,1,
                                        0,0,1,
                                        0,0,1};
  private float[] colours = new float[]{1,1,1,1,
                                        1,1,1,1,
                                        1,1,1,1,
                                        1,1,1,1};
  
  /**
   * Constructs a new <tt>MultiTex</tt> object using the textures
   * identified in <tt>textures</tt>.</br>
   * The index of the textures in the list will be the texture unit
   * that the texture is bound to.</br>
   * @param textures a list of paths to the textures to use.
   * @throws SlickException If <tt>textures.size()</tt> is greater
   * than the maximum number of texture units.
   */
  public MultiTex(List<String> textures)throws SlickException{
    //Check how many texture units are supported 
    if(units==-1){
      units = GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS);
      System.out.println(units);
    }
    if(units < textures.size()){
      throw new UnsupportedOperationException("You attempted to " +
      		"create an artifact with " + textures.size() +
      		" textures, but your environment only supports " +
      		units + " texure image units.");
    }
    
    //Create texture list
    this.textures = new ArrayList<Texture>(textures.size());
    
    
     
    //Load textures into texture list.
    InternalTextureLoader itl = InternalTextureLoader.get();
    for(int i = 0; i<textures.size(); i++){
      GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      try{
        this.textures.add(itl.getTexture(textures.get(i),
                                         false,
                                         GL11.GL_LINEAR,
                                         null));
      }catch(IOException e){
        throw new SlickException(e.getMessage());
      }
      GL11.glDisable(GL11.GL_TEXTURE_2D);
    }
    //FIXME pretty sure there is a rather serious problem here.
    //Since the TextureLoader used keeps track of previously loaded
    //textures, and binds them to a unit at creation.  If a single
    //image is loaded twice to two different Texture Units, it may
    //not actually be associated with the correct unit on the
    //second load.  This is because the TextureLoader will simply
    //return the earlier loaded texture.
    
    //Reset current texture unit to 0
    GL13.glActiveTexture(GL13.GL_TEXTURE0);
    
    imgWidth  = this.textures.get(primaryTextureIndex).getImageWidth();
    imgHeight = this.textures.get(primaryTextureIndex).getImageHeight();
    texWidth  = this.textures.get(primaryTextureIndex).getWidth();
    texHeight = this.textures.get(primaryTextureIndex).getHeight();
  }
  
  
  
  public MultiTex(String[] textures) throws SlickException{
    this(Arrays.asList(textures));
  }
  
  
  
  public MultiTex(String t1, String t2)throws SlickException{
    this(new String[]{t1,t2});
  }
  
  
  
  /**
   * When extending please note that this method relies on the
   * private method drawEmbedded.</br>
   */
  public void draw(float x, float y){
      draw(x, y, x + imgWidth, y+imgHeight,
           0, 0, imgWidth, imgHeight);

  }
  
  
  
  /**
   * Draw a section of this MultTex at a particular location and
   * scale on the screen.</br>
   * 
   * This is the draw method that all other overloaded draw
   * methods eventually evoke.</br>
   * 
   * @param x1
   * @param y1
   * @param x2
   * @param y2
   * @param sx1
   * @param sy1
   * @param sx2
   * @param sy2
   */
  public void draw(float x1, float y1, float x2, float y2,
                   float sx1, float sy1, float sx2, float sy2){
    
    //Bind textures to their correct locations
    for(int i = 0; i < textures.size(); i++){
      GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures.get(i).getTextureID());
    }  
    
    GL11.glBegin(GL11.GL_QUADS);
      drawEmbedded(x1, y1, x2, y2,
                   sx1, sy1, sx2, sy2);
    GL11.glEnd();
      
    //Clean up texture setting to allow basic slick to operate correctly.
    for(int i = textures.size()-1; i>=0; i--){
      GL13.glActiveTexture(GL13.GL_TEXTURE0+i);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
    }
    GL11.glEnable(GL11.GL_TEXTURE_2D);
  }
  
  
  
  public void draw(float x1, float y1, float x2, float y2,
                   float sx1, float sy1, float sx2, float sy2,
                   Color c){
    float[] bu = colours;//Save the colour state
    
    setColour(c);
    draw(x1, y1, x2, y2, sx1, sy1, sx2, sy2);
    
    colours = bu;//Restore the colour state
  }

  
  
  /**
   * Sets the colour of a given corner.</br>
   * Note that this will have an effect only if: the
   * fixed pixel pipeline is being used; or the applied shader
   * takes the vertex colour into account.</br>  
   * @param corner
   * @param c
   */
  public void setColour(int corner, Color c){
    colours[corner*4 + 0] = c.r;
    colours[corner*4 + 1] = c.g;
    colours[corner*4 + 2] = c.b;
    colours[corner*4 + 3] = c.a;
  }
  
  
  
  /**
   * Sets the colour of all four corners.</br>
   * @param c
   */
  public void setColour(Color c){
    for(int i=0; i<4; i++){
      setColour(i, c);
    }
  }

  
  
  private void drawEmbedded(float x1, float y1, float x2, float y2,
                            float sx1, float sy1, float sx2, float sy2){
    //TODO reduce code duplication need to produce sequence 0,1,3,2

    //TOP LEFT
    for(int i=0; i<textures.size(); i++){
      GL13.glMultiTexCoord2f(GL13.GL_TEXTURE0 + i,
                             (sx1/imgWidth) * texWidth,
                             (sy1/imgHeight)* texHeight);
    }
    GL11.glColor4f(colours[0], colours[1], colours[2], colours[3]);
    GL11.glNormal3f(normals[0], normals[1], normals[2]);
    GL11.glVertex3f(x1, y1, 0); 
    
    //BOTTOM LEFT
    for(int i=0; i<textures.size(); i++){
      GL13.glMultiTexCoord2f(GL13.GL_TEXTURE0 + i,
                             (sx1/imgWidth) * texWidth,
                             (sy2/imgHeight)* texHeight);
    }
    GL11.glColor4f(colours[3], colours[5], colours[6], colours[7]);
    GL11.glNormal3f(normals[3], normals[4], normals[5]);
    GL11.glVertex3f(x1, y2, 0); 
    
    //BOTTOM RIGHT
    for(int i=0; i<textures.size(); i++){
      GL13.glMultiTexCoord2f(GL13.GL_TEXTURE0 + i,
                             (sx2/imgWidth) * texWidth,
                             (sy2/imgHeight)* texHeight);
    }
    GL11.glColor4f(colours[8], colours[9], colours[10], colours[11]);
    GL11.glNormal3f(normals[6], normals[7], normals[8]);
    GL11.glVertex3f(x2, y2, 0); 
    
    //TOP RIGHT
    for(int i=0; i<textures.size(); i++){
      GL13.glMultiTexCoord2f(GL13.GL_TEXTURE0 + i,
                             (sx2/imgWidth) * texWidth,
                             (sy1/imgHeight)* texHeight);
    }
    GL11.glColor4f(colours[12], colours[13], colours[14], colours[15]);
    GL11.glNormal3f(normals[9], normals[10], normals[11]);
    GL11.glVertex3f(x2, y1, 0); 
  }


  
}
