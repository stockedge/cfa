package jp.dip.ysfactory.cfa;

/*
 * Copyright (C) 2015 Yasumasa Suenaga
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import java.nio.file.Path;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import java.util.stream.Collectors;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;


/**
 * Dump class information.
 */
public class ClassInfoDumper implements Dumper{

  /**
   * Field information.
   */
  public static class FieldInfo{

    /**
     * Field type.
     */
    private String type;

    /**
     * Class which is included this field.
     */
    private String className;

    /**
     * Field name.
     */
    private String name;

    public FieldInfo(ConstantPool.CPRefInfo ref){
      try{
        type = ref.getNameAndTypeInfo().getType().replace('/', '.');
        className = ref.getClassName().replace('/', '.');
        name = ref.getNameAndTypeInfo().getName();
      }
      catch(ConstantPoolException e){
        throw new RuntimeException(e);
      }
    }

    public String getType(){
      return type;
    }

    public String getClassName(){
      return className;
    }

    public String getName(){
      return name;
    }

    @Override
    public String toString(){
      return type + " " + className + "." + name;
    }

  }

  /**
   * Method information.
   */
  public static class MethodInfo{

    /**
     * Class which is included this method.
     */
    private String className;

    /**
     * Method name.
     */
    private String name;

    /**
     * Method signature.
     */
    private String signature;

    public MethodInfo(ConstantPool.CPRefInfo ref){
      try{
        className = ref.getClassName().replace('/', '.');
        name = ref.getNameAndTypeInfo().getName();
        signature = ref.getNameAndTypeInfo().getType();
      }
      catch(ConstantPoolException e){
        throw new RuntimeException(e);
      }
    }

    public String getClassName(){
      return className;
    }

    public String getName(){
      return name;
    }

    public String getSignature(){
      return signature;
    }

    @Override
    public String toString(){
      return className + "." + name + signature;
    }

  }

  /**
   * ClassFile instance of this class.
   */
  private ClassFile clazz;

  /**
   * File name which is included in this class.
   */
  private String fname;

  /**
   * Class name.
   */
  private String className;

  /**
   * Super class of this class.
   */
  private String superClass;

  /**
   * Interface list of this class.
   */
  private List<String> interfaceList;

  /**
   * Field list of this class.
   */
  private List<FieldInfo> fieldList;

  /**
   * Method list of this class.
   */
  private List<MethodInfo> methodList;

  /**
   * Class collection of this class.
   */
  private Set<String> classSet;

  /**
   * Constructor of ClassInfoDumper.
   *
   * @param path Path of class file.
   */
  public ClassInfoDumper(Path path) throws IOException, ConstantPoolException{
    clazz = ClassFile.read(path);
    fname = path.toString();
    initialize();
  }

  /**
   * Constructor of ClassInfoDumper.
   *
   * @param in InputStream of class.
   * @param fname File name or archive of class.
   */
  public ClassInfoDumper(InputStream in, String fname)
                               throws IOException, ConstantPoolException{
    clazz = ClassFile.read(in);
    this.fname = fname;
    initialize();
  }

  /**
   * Initialize class information.
   */
  private void initialize(){

    try{
      className = clazz.getName().replace('/', '.');
      superClass = clazz.getSuperclassName().replace('/', '.');
    }
    catch(ConstantPoolException e){
      throw new RuntimeException(e);
    }

    interfaceList = IntStream.range(0, clazz.interfaces.length)
                             .mapToObj(i -> {
                                              try{
                                                return clazz.getInterfaceName(i);
                                              }
                                              catch(ConstantPoolException e){
                                                throw new RuntimeException(e);
                                              }
                                            })
                             .map(c -> c.replace('/', '.'))
                             .collect(Collectors.toList());
    fieldList = StreamSupport.stream(
                          clazz.constant_pool.entries().spliterator(), false)
                  .filter(p -> p instanceof ConstantPool.CONSTANT_Fieldref_info)
                  .map(p -> new FieldInfo((ConstantPool.CPRefInfo)p))
                  .collect(Collectors.toList());
    methodList = StreamSupport.stream(
                          clazz.constant_pool.entries().spliterator(), false)
                  .filter(p -> 
                   (p instanceof ConstantPool.CONSTANT_Methodref_info) ||
                   (p instanceof ConstantPool.CONSTANT_InterfaceMethodref_info))
                  .map(p -> new MethodInfo((ConstantPool.CPRefInfo)p))
                  .collect(Collectors.toList());

    classSet = new HashSet<>();

    if(superClass != null){
      classSet.add(superClass);
    }

    interfaceList.forEach(classSet::add);
    fieldList.forEach(e -> {
                             classSet.add(e.getType());
                             classSet.add(e.getClassName());
                           });
    methodList.forEach(e -> classSet.add(e.getClassName()));
  }

  /**
   * Print class information as below:
   * <ul>
   *   <li>Class name</li>
   *   <li>File name</li>
   *   <li>Super class</li>
   *   <li>Interfaces</li>
   *   <li>Class version</li>
   * </ul>
   */
  public void printClassInfo(boolean isShort){
    System.out.println("Name: " + className);
    System.out.println("File: " + fname);

    if(isShort){
      return;
    }

    System.out.println("Super class: " + superClass);

    System.out.println("Interfaces:");
    interfaceList.forEach(e -> System.out.println("  " + e));

    System.out.print(String.format("Class version: %d.%d ",
                                     clazz.major_version, clazz.minor_version));
    if(clazz.minor_version == 0){
      switch(clazz.major_version){
        case 46: System.out.println("(Java 1.2)");
                 break;
        case 47: System.out.println("(Java 1.3)");
                 break;
        case 48: System.out.println("(Java 1.4)");
                 break;
        case 49: System.out.println("(Java 1.5)");
                 break;
        case 50: System.out.println("(Java 6)");
                 break;
        case 51: System.out.println("(Java 7)");
                 break;
        case 52: System.out.println("(Java 8)");
                 break;
        default: System.out.println("(Unknown)");
      }
    }
    else{
      System.out.println("(Unknown)");
    }

  }

  /**
   * Print field information.
   */
  public void printFieldRefInfo(){
    System.out.println("Field References:");
    fieldList.forEach(e -> System.out.println("  " + e.toString()));
  }

  /**
   * Print method information.
   */
  public void printMethodRefInfo(){
    System.out.println("Method References:");
    methodList.forEach(e -> System.out.println("  " + e.toString()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dumpInfo(Option option){

    if(option.getTargetList() != null){
      if(!option.getTargetList()
                .stream()
                .anyMatch(t -> className.contains(t))){
        return;
      }
    }

    if(option.getClassFilterList() != null){
      if(!option.getClassFilterList()
                .stream()
                .anyMatch(t -> classSet.stream()
                                       .anyMatch(c -> c.contains(t)))){
        return;
      }
    }

    if(option.getMethodFilterList() != null){
      if(!option.getMethodFilterList()
                .stream()
                .anyMatch(t -> methodList.stream()
                                         .map(e -> e.toString())
                                         .anyMatch(m -> m.contains(t)))){
        return;
      }
    }

    printClassInfo(option.isShort());

    if(!option.isShort()){
      printFieldRefInfo();
      printMethodRefInfo();
    }

    System.out.println();
  }

}

