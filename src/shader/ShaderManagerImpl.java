package shader;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.ResourceLoader;

/**
 * A simple class used to prevent duplicate shaders from
 * being loaded and compiled onto the video card.</br>
 * @author Chronocide (Jeremy Klix)
 */
class ShaderResourceManagerImpl implements ShaderResourceManager{
  private static final ShaderResourceManager SRM = new ShaderResourceManagerImpl();

  
  private Map<String, Integer> shaderMap = new HashMap<String, Integer>();
  private Map<Integer, Set<Integer>> shaderToPrograms = new HashMap<Integer, Set<Integer>>(); //for every shader lists all the programs that use it
  private Map<Integer, Set<Integer>> programToShaders = new HashMap<Integer, Set<Integer>>(); //for every program lists the shaders it uses
  
  
  //Constructor
  private ShaderResourceManagerImpl(){
    //Private Constructor to prevent external extension
  }
  
  
  
  //Factory Method
  static ShaderResourceManager getSRM(){
    return SRM;
  }
  
  
  
  /**
   * Fetches a shader id for a given fragment shader, generating
   * a new id if necessary.</br>
   * 
   * @param fragmentFileName the fragment shader to fetch an id for.
   * @returns shaderID for a given fragment shader.</br>
   */
  public int getFragementShaderID(String fragmentFileName)throws SlickException{
    Integer id = shaderMap.get(fragmentFileName);
    if(id==null){
      id = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
      shaderMap.put(fragmentFileName, id);
      
      GL20.glShaderSource(id, getProgramCode(fragmentFileName));
      GL20.glCompileShader(id);
    }
    return id;
  }
  
  
  
  /**
   * Fetches a shader id for a given vertex shader, generating
   * a new id if necessary.</br>
   * 
   * @param vertexFileName the vertex shader to fetch an id for.
   * @returns shaderID for a given vertex shader.</br>
   */
  public int getVertexShaderID(String vertexFileName)throws SlickException{
    Integer id = shaderMap.get(vertexFileName);
    if(id==null){
      id = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
      shaderMap.put(vertexFileName, id);
      
      GL20.glShaderSource(id, getProgramCode(vertexFileName));
      GL20.glCompileShader(id);
    }
    return id;
  }
  
  
  
  /**
   * Link a shader that the shader program depends on to operate.</br>
   * @param programID
   * @param shaderID
   */
  public void createProgramShaderDependancy(int programID,
                                            int shaderID){
    if(!shaderMap.containsValue(shaderID)){
      throw new IllegalArgumentException("Cannot link a shader " +
      	                                 "id that does not exist.");
    }
    
    //Add Shader to list of shaders used by program
    Set<Integer> shaders = programToShaders.get(programID);
    if(shaders==null){
      shaders = new HashSet<Integer>();
      programToShaders.put(programID, shaders);
    }
    shaders.add(shaderID);
    
    //Add program to list of programs used by Shader
    Set<Integer> programs = shaderToPrograms.get(shaderID);
    if(programs==null){
      programs = new HashSet<Integer>();
      shaderToPrograms.put(shaderID, programs);
    }
    programs.add(programID);
  }
  
  
  
  public void createProgramShaderDependancies(int programID,
                                              Iterable<Integer> ids){
    for(int id : ids){
      createProgramShaderDependancy(programID, id);
    }
  }
  
  
  
  /**
   * Removes the program.</br>
   * After calling this method the program specified will no longer
   * be loaded, nor will any shaders that were still in use only by
   * only that program.</br>
   * 
   * @params programID the ID of the program to remove.
   */
  //This is a rather inefficient implementation however, it need not
  //be fast and this representation is very simple to follow and
  //debug.
  public void removeProgram(int programID){
    Set<Integer> shaders = programToShaders.get(programID);
    if(shaders==null){
      throw new IllegalArgumentException("The programID " +
                                         programID +
                                         "does not exist");
    }    

    //detach Shaders from program
    for(int id : shaders){
      GL20.glDetachShader(programID, id); 
    }
    
    //Delete unused shaders
    for(int id : shaders){
      Set<Integer> progs = shaderToPrograms.get(id);
      progs.remove(programID);
      if(progs.isEmpty()){
        GL20.glDeleteShader(id);
        shaderToPrograms.remove(id);
      }
    }
    
    //Delete Program
    GL20.glDeleteProgram(programID);
    programToShaders.remove(programID);
  }
  
  
  
  /**
   * Gets the program code from the file <tt>filename</tt> and puts
   * it into a <tt>ByteBuffer</tt>.</br>
   * @param filename the full name of the file.
   * @return a ByteBuffer containing the program code.
   * @throws SlickException
   */
  private ByteBuffer getProgramCode(String filename)throws SlickException{
    InputStream fileInputStream = null;
    byte[] shaderCode = null;
        
    fileInputStream = ResourceLoader.getResourceAsStream(filename);
    DataInputStream dataStream = new DataInputStream(fileInputStream);
    try{
      dataStream.readFully(shaderCode = new byte[fileInputStream.available()]);
      fileInputStream.close();
      dataStream.close();
    }catch (IOException e) {
      throw new SlickException(e.getMessage());
    }

    ByteBuffer shaderPro = BufferUtils.createByteBuffer(shaderCode.length);

    shaderPro.put(shaderCode);
    shaderPro.flip();

    return shaderPro;
  }
  
}
