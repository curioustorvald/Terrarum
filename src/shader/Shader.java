package shader;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.util.ResourceLoader;


/**
 * Class used to use and access shaders without having to deal
 * with all of the fidly openGL bits.
 * @author Chronocide (Jeremy Klix)
 *
 */
public class Shader {
  public static final int BRIEF = 128;
  public static final int MODERATE = 512;
  public static final int VERBOSE = 1024;  
  private static final int NOT_LOADED = -1;
  private static final String ERR_LOCATION =
    "Warning: variable %s could not be found. " +
    "Ensure the name is spelled correctly\n";
  private static int logging = MODERATE;
  
  private ShaderResourceManager srm;
  /**
   * ID of the <tt>Shader</tt>.  A Shader may have programID of 
   * -1 only before construction is completed, or
   * after the <tt>Shader</tt> is deleted
   */
  private int programID = NOT_LOADED;
  private Map<String, ShaderVariable> vars = new HashMap<String, ShaderVariable>();
  
  
  private Shader(ShaderResourceManager srm,
                 Collection<String> vertex,
                 Collection<String> fragment)throws SlickException{
    this.srm = srm;
    StringBuilder errorMessage = new StringBuilder();
    
    programID = GL20.glCreateProgram();
    
    int[] shaderIds = new int[vertex.size() + fragment.size()];
    int index = 0;
    
    //Load Vertex Shaders
    for(String vertShader: vertex){      
      int vsid = srm.getVertexShaderID(vertShader);
      srm.createProgramShaderDependancy(programID, vsid);
      
      //Add to shader ids array
      shaderIds[index] = vsid;
      index++;
      
      //Check for errors with shader
      if(!compiledSuccessfully(vsid)){
        errorMessage.append("Vertex Shader ");
        errorMessage.append(vertShader);
        errorMessage.append(" failed to compile.\n");
        errorMessage.append(getShaderInfoLog(vsid));
        errorMessage.append("\n\n");
      }
      
      scanSource(vertShader);
    }
    
    
    //Load Fragment Shaders
    for(String fragShader: fragment){      
      int fsid = srm.getFragementShaderID(fragShader);
      srm.createProgramShaderDependancy(programID, fsid);

      //Add to shader ids array
      shaderIds[index] = fsid;
      index++;
      
      //Check for errors with shader
      if(!compiledSuccessfully(fsid)){
        errorMessage.append("Fragment Shader ");
        errorMessage.append(fragShader);
        errorMessage.append(" failed to compile.\n");
        errorMessage.append(getShaderInfoLog(fsid));
        errorMessage.append("\n\n");
      }
      
      scanSource(fragShader);
    }
    
    //Attach shaders to program
    for(int i=0; i<index; i++){
      GL20.glAttachShader(programID, shaderIds[i]);
    }
    //Link program
    GL20.glLinkProgram(programID);
    if(!linkedSuccessfully()){
      errorMessage.append("Linking Error\n");
      errorMessage.append(getProgramInfoLog());
      errorMessage.append("\n\n");
    }
    
    if(errorMessage.length()!=0){
      errorMessage.insert(0, "Could not compile shader.\n");
      srm.removeProgram(programID);
      programID = -1;
      errorMessage.append("Stack Trace:");
      throw new SlickException(errorMessage.toString());
    }
    
    
  }
  
  
  
  /**
   * Factory method to create a new Shader.
   * @param vertexFileName
   * @param fragmentFileName
   * @return
   * @throws SlickException
   */
  public static Shader makeShader(String vertexFileName,
                                  String fragmentFileName)throws SlickException{
    ArrayList<String> l1 = new ArrayList<String>();
    l1.add(vertexFileName);
    ArrayList<String> l2 = new ArrayList<String>();
    l2.add(fragmentFileName);
    
    return new Shader(ShaderResourceManagerImpl.getSRM(),
                      l1,
                      l2);
  }
  
  
  
  /**
   * Reverts GL context back to the fixed pixel pipeline.<br>
   */
  public static void forceFixedShader(){
    GL20.glUseProgram(0);
  }
  
  
  
  /**
   * Sets the number of characters to be returned when printing
   * errors.</br>  Suggested values are the constants
   * <tt>BRIEF</tt>, <tt>MODERATE</tt>, and <tt>VERBOSE</tt>.</br>
   * @param detailLevel number of characters to display for error
   *                    messages.
   */
  public static void setLoggingDetail(int detailLevel){
    logging = detailLevel;
  }

  
  
  /**
   * Deletes this shader and unloads all free resources.</br>
   * TODO should this be called from <tt>finalise()</tt>, or is
   * that just asking for trouble?
   */
  public void deleteShader(){
    srm.removeProgram(programID);
    programID = NOT_LOADED;
  }
  
  
  
  /**
   * Returns true if this <tt>Shader</tt> has been deleted.</br>
   * @return true if this <tt>Shader</tt> has been deleted.</br>
   */
  public boolean isDeleted(){
    return programID == NOT_LOADED;
  }
  
  
  
  /**
   * Activates the shader.</br>
   */
  public void startShader(){
    if(programID == NOT_LOADED){
      throw new IllegalStateException("Cannot start shader; this" +
                                      " Shader has been deleted");
    }
    forceFixedShader(); //Not sure why this is necessary but it is.
    GL20.glUseProgram(programID);
  }
  
  
  
//UNIFORM SETTERS  
  /**
   * Sets the value of the uniform integer Variable <tt>name</tt>.</br>
   * @param name the variable to set.
   * @param value the value to be set.
   */
  public Shader setUniformIntVariable(String name, int value){
  	return setUniformIntVariable(name, new int[]{value});
  }
  
  
  
  public Shader setUniformIntVariable(String name, int v0, int v1){
  	return setUniformIntVariable(name, new int[]{v0, v1});
  }
  
  
  
  public Shader setUniformIntVariable(String name,
                                      int v0, int v1, int v2){
  	return setUniformIntVariable(name, new int[]{v0, v1, v2});
  }
  
  
  
  public Shader setUniformIntVariable(String name,
                                      int v0, int v1, int v2, int v3){
    return setUniformIntVariable(name, new int[]{v0, v1, v2, v3});
  }
  
  
  public Shader setUniformIntVariable(String name, int[] values){
	  ShaderVariable var = vars.get(name);
	  if(var==null){
	    printError(name);
	  }else{
	    var.setUniformValue(values);
	  }
  	return this;
  }

  
  
  /**
   * Sets the value of the uniform integer Variable
   * <tt>name</tt>.</br>
   * @param name the variable to set.
   * @param value the value to be set.
   */
  public Shader setUniformFloatVariable(String name, float value){
  	return setUniformFloatVariable(name, new float[]{value});
  }
  
  
  
  public Shader setUniformFloatVariable(String name,
                                        float v0, float v1){
  	return setUniformFloatVariable(name, new float[]{v0, v1});
  }
  
  
  
  public Shader setUniformFloatVariable(String name,
                                        float v0, float v1, float v2){
  	return setUniformFloatVariable(name, new float[]{v0, v1, v2});
  }
  
  
  
  public Shader setUniformFloatVariable(String name,
                                        float v0, float v1,
                                        float v2, float v3){
  	return setUniformFloatVariable(name, new float[]{v0, v1, v2, v3});
  }
  
  
  
  public Shader setUniformFloatVariable(String name, float[] values){
	  ShaderVariable var = vars.get(name);
	  if(var==null){
	    printError(name);
	  }else{
	    var.setUniformValue(values);
	  }
  	return this;
  }
  
  
  
  //TODO implement using ShaderVariable
  //TODO Test
  public Shader setUniformMatrix(String name,
                               boolean transpose,
                               float[][] matrix){
    //Convert matrix format
    FloatBuffer matBuffer = matrixPrepare(matrix);
    
    //Get uniform location
    int location = GL20.glGetUniformLocation(programID, name);
    printError(name);

    //determine correct matrixSetter
    switch(matrix.length){
      case 2: GL20.glUniformMatrix2(location, transpose, matBuffer);
      break;
      case 3: GL20.glUniformMatrix3(location, transpose, matBuffer);
      break;
      case 4: GL20.glUniformMatrix4(location, transpose, matBuffer);
      break;
    }
    
    return this;
  }
  
  
   
  private FloatBuffer matrixPrepare(float[][] matrix){
    //Check argument validity
    if(matrix==null){
      throw new IllegalArgumentException("The matrix may not be null");
    }
    int row = matrix.length;
    if(row<2){
      throw new IllegalArgumentException("The matrix must have at least 2 rows.");
    }
    int col = matrix[0].length;
    if(col!=row){
      throw new IllegalArgumentException("The matrix must have an equal number of rows and columns.");
    }
    float[] unrolled = new float[row*col];
    
    for(int i=0;i<row;i++){
      for(int j=0;j<col;j++){
        unrolled[i*col+j] = matrix[i][j];
      }
    }

    //TODO FloatBuffer creation here is probably broken
    return FloatBuffer.wrap(unrolled);
  }
  
  
  
  private void printError(String varName){
      System.err.printf(ERR_LOCATION, varName);
  }
  
  
  
  /**
   * Returns true if the shader compiled successfully.</br>
   * @param shaderID
   * @return true if the shader compiled successfully.</br>
   */
  private boolean compiledSuccessfully(int shaderID){
    return GL20.glGetShader(shaderID, GL20.GL_COMPILE_STATUS)==GL11.GL_TRUE;
  }

  
  
  /**
   * Returns true if the shader program linked successfully.</br>
   * @return true if the shader program linked successfully.</br>
   */
  private boolean linkedSuccessfully(){
	  int test = GL20.glGetShader(programID, GL20.GL_LINK_STATUS);
	  return true;
//    return GL20.glGetShader(programID, GL20.GL_LINK_STATUS)==GL11.GL_TRUE;
  }
  
  
  
  private String getShaderInfoLog(int shaderID){
    return GL20.glGetShaderInfoLog(shaderID, logging).trim();
  }
  
  
  
  private String getProgramInfoLog(){
    return GL20.glGetProgramInfoLog(programID, logging).trim();
  }
  
  
  
  private void scrapeVariables(String varLine){
    ShaderVariable.Qualifier qualifier = null;
    ShaderVariable.Type type = null;
    String name  = "";
    int    vecSize = 1; // if a vector the
    int    size = 1; //If array size of array
    
    String str;
  	Scanner scanner = new Scanner(varLine);
  	scanner.useDelimiter("[\\s,]++");
  	
  	//Determine qualifier
  	qualifier = ShaderVariable.Qualifier.fromString(scanner.next());
  	
  	//Determine type
  	str = scanner.next();
  	if(str.equals("float")){
  	  type = ShaderVariable.Type.FLOAT;
  	}else if(str.matches("[u]?int|sampler[123]D")){
  	  type = ShaderVariable.Type.INTEGER;
  	}else if(str.equals("bool")){
  	  type = ShaderVariable.Type.BOOLEAN;
  	}else if(str.matches("[bdiu]?vec[234]")){
  	  char c = str.charAt(0);
  	  switch(c){
  	    case 'b':
  	      type = ShaderVariable.Type.BOOLEAN; break;
  	    case 'd':
  	      type = ShaderVariable.Type.DOUBLE; break;
  	    case 'i':
  	    case 'u':
          type = ShaderVariable.Type.INTEGER; break;
  	    case 'v':
  	      type = ShaderVariable.Type.FLOAT; break;
  	  }
  	  
  	  str = str.substring(str.length()-1);
  	  vecSize = Integer.parseInt(str);
  	}
  	
  	
  	//Determine variable names
  	while(scanner.hasNext("[\\w_]+[\\w\\d_]*(\\[\\d+\\])?")){
  		name = scanner.next();
  		if(name.contains("]")){
  			String sub = name.substring(name.indexOf('[')+1, name.length()-1);
  			size = Integer.parseInt(sub);
  			name = name.substring(0, name.indexOf('[')).trim();
  		}
  		
  	  ShaderVariable var =
  	    new ShaderVariable(programID,
  	                       name, qualifier, type, vecSize, size);
  	  vars.put(var.name, var);
  	}
  }
  
  
  
  private void scanSource(String filename){
    Scanner scanner = new Scanner(ResourceLoader.getResourceAsStream(filename));
    scanner.useDelimiter(";|\\{|\\}");
    while(scanner.hasNext()){
      while(scanner.hasNext("\\s*?(uniform|attribute|varying).*")){
        scrapeVariables(scanner.next().trim());
      }
      scanner.next();
    }
  }
}
