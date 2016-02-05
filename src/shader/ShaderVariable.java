package shader;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;


/**
 * Class used to keep track of variables associated with this
 * shader.</br>
 * 
 * @author Chronocide (Jeremy Klix)
 *
 */
class ShaderVariable{
  public enum Qualifier{
    ATTRIBUTE("attribute"), UNIFORM("uniform"), VARYING("varying");
    
    private static final Map<String, ShaderVariable.Qualifier> stringToEnum =
      new HashMap<String, ShaderVariable.Qualifier>();
    
    static{
      for(ShaderVariable.Qualifier qual : values()){
        stringToEnum.put(qual.toString(),qual);
      }
    }
    
    private String name;
    
    
    Qualifier(String name){
      this.name = name;
    }
    
    public String toString(){
      return name;
    }
    
    public static ShaderVariable.Qualifier fromString(String token){
      return stringToEnum.get(token);
    }
  }
	
  public enum Type{
    BOOLEAN("boolean"), DOUBLE("double"), FLOAT("float"),
    INTEGER("integer");
    
    private String name;
    
    Type(String name){
      this.name = name;
    }
    
    public String toString(){
      return name;
    }
    
  }
  
  private static final String TYPE_WARN =
    "Warning!\nProblem setting %s variable. " +
    "Expected type %s but got type %s instead.\n";
  private static final String QUAL_WARN =
    "Warning!\nProblem setting %s variable. " +
    "Expected qualifier %s but got %s instead.\n";
  
  private ShaderVariable.Qualifier qualifier = null;
  private ShaderVariable.Type type = null;
  private int vecSize; //size of vector
  private int size; //size of array non arrays are size 1
  private int programID;
  
  
  private int location = -1;
  
  /**Set true if GLSL has removed this unused variable*/
  private boolean isCulled = false;
  String name = "";
  
  ShaderVariable(int programID, String name,
                 ShaderVariable.Qualifier qual,
                 ShaderVariable.Type type,
                 int vecSize,
                 int size){
    this.programID = programID;
    this.name = name;
    
    this.qualifier = qual;
    this.type      = type;
    if(vecSize<1){
      throw new IllegalArgumentException("size of elements must be greater than 0");
    }
    this.vecSize = vecSize;
    if(size<1){
      throw new IllegalArgumentException("number of elements must be greater than 0");
    }
    this.size = size;
  }
  
  
  
  public String toString(){
    return name;
  }
  
  
  
  public boolean equals(Object obj){
    if(obj instanceof ShaderVariable){
      return this.toString().equals(obj.toString());
    }
    return false;
  }
  
  
  
  public int hashCode(){
    return name.hashCode();
  }
 
  
  //TODO TEST
  void setUniformValue(boolean[] vals){
    if(this.type!=Type.BOOLEAN){
      System.err.printf(TYPE_WARN, this.name, this.type, Type.BOOLEAN);
    }
    if(this.qualifier!=Qualifier.UNIFORM){
      System.err.printf(QUAL_WARN, this.name, this.qualifier, Qualifier.UNIFORM);
    }
    if(vals.length!=vecSize){
      throw new AssertionError("Incorrect number of arguments.");
    }
    
    //No GL methods to set boolean uniforms exist
    if(location==-1){
      CharSequence param = new StringBuffer(name);
      location = GL20.glGetUniformLocation(programID, param);
      locationCheck();
    }
    
    IntBuffer fb = BufferUtils.createIntBuffer(vals.length);
		for(boolean b : vals){
			fb.put(b? 1 : 0);
		}
		fb.flip();
    switch(vecSize){
    	case 1: GL20.glUniform1(location, fb); break;
    	case 2: GL20.glUniform2(location, fb); break;
    	case 3: GL20.glUniform3(location, fb); break;
    	case 4: GL20.glUniform4(location, fb); break;
    }
  }
  
  
  
  void setUniformValue(float[] vals){
    if(this.type!=Type.FLOAT){
      System.err.printf(TYPE_WARN, this.name, this.type, Type.FLOAT);
    }
    if(this.qualifier!=Qualifier.UNIFORM){
      System.err.printf(QUAL_WARN, this.name, this.qualifier, Qualifier.UNIFORM);
    }
    if(vals.length!=vecSize*size){
      throw new AssertionError("Incorrect number of values.\n" +
      		"Expected " + vecSize*size + " vlaues but got " +
      		vals.length + " values instead.");
    }
    
    if(location==-1){
      CharSequence param = new StringBuffer(name);
      location = GL20.glGetUniformLocation(programID, param);
      locationCheck();
    }
    
    FloatBuffer fb = BufferUtils.createFloatBuffer(vals.length);
		fb.put(vals);
		fb.flip();
    switch(vecSize){
    	case 1: GL20.glUniform1(location, fb); break;
    	case 2: GL20.glUniform2(location, fb); break;
    	case 3: GL20.glUniform3(location, fb); break;
    	case 4: GL20.glUniform4(location, fb); break;
    }
  }
  
  
  
  void setUniformValue(int[] vals){
    if(this.type!=Type.INTEGER){
      System.err.printf(TYPE_WARN, this.type, Type.INTEGER);
    }
    if(this.qualifier!=Qualifier.UNIFORM){
      System.err.printf(QUAL_WARN, this.name, this.qualifier, Qualifier.UNIFORM);
    }
    if(vals.length!=vecSize*size){
      throw new AssertionError("Incorrect number of values.\n" +
      		"Expected " + vecSize*size + " vlaues but got " +
      		vals.length + " values instead.");
    }
    
    if(location==-1){
      CharSequence param = new StringBuffer(name);
      location = GL20.glGetUniformLocation(programID, param);
      locationCheck();
    }
    
    IntBuffer fb = BufferUtils.createIntBuffer(vals.length);
		fb.put(vals);
		fb.flip();
    switch(vecSize){
    	case 1: GL20.glUniform1(location, fb); break;
    	case 2: GL20.glUniform2(location, fb); break;
    	case 3: GL20.glUniform3(location, fb); break;
    	case 4: GL20.glUniform4(location, fb); break;
    }
  }
  
  
  
  private void locationCheck(){
    if(location==-1 && !isCulled){
      System.err.println("Location for variable " + name +
                         "could not be found.\nGLSL may remove " +
                         "any vairable that does not contribute " +
                         "to an output. Check and ensure " +
                         "that " + name + "is an active variable.\n");
      isCulled = true;
    }
  }
}