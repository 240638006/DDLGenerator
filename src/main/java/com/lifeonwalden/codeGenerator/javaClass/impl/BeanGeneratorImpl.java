package com.lifeonwalden.codeGenerator.javaClass.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.lang.model.element.Modifier;

import com.lifeonwalden.codeGenerator.bean.Column;
import com.lifeonwalden.codeGenerator.bean.Table;
import com.lifeonwalden.codeGenerator.bean.config.Config;
import com.lifeonwalden.codeGenerator.constant.JdbcTypeEnum;
import com.lifeonwalden.codeGenerator.javaClass.BeanGenerator;
import com.lifeonwalden.codeGenerator.util.StringUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

public class BeanGeneratorImpl implements BeanGenerator {

  @Override
  public void generate(Table table, Config config) {
    String className = StringUtil.firstAlphToUpper(table.getName());
    ClassName _className = ClassName.get(config.getBeanInfo().getPackageName(), className);
    Builder dtoTypeBuilder =
        TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ClassName.get(Serializable.class));

    dtoTypeBuilder.addField(FieldSpec
        .builder(long.class, "serialVersionUID", Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
        .initializer("$L$L", System.currentTimeMillis(), "L").build());

    for (Column column : table.getColumns()) {
      String javaType = column.getJavaType();
      if (null == javaType) {
        JdbcTypeEnum jdbcType = JdbcTypeEnum.nameOf(column.getType().toUpperCase());
        if (null == jdbcType) {
          throw new RuntimeException("unknow jdbc type : " + column.getType().toUpperCase());
        }
        javaType = jdbcType.getJavaType();
      }

      ClassName javaTypeClassName = ClassName.bestGuess(javaType);
      dtoTypeBuilder.addField(FieldSpec.builder(javaTypeClassName, column.getName(), Modifier.PRIVATE)
          .addJavadoc("$L", column.getNote()).build());

      dtoTypeBuilder.addMethod(MethodSpec.methodBuilder("get" + StringUtil.firstAlphToUpper(column.getName()))
          .addModifiers(Modifier.PUBLIC).returns(javaTypeClassName).addStatement("return this.$L", column.getName())
          .addJavadoc("$L", column.getNote()).build());

      dtoTypeBuilder.addMethod(MethodSpec.methodBuilder("set" + StringUtil.firstAlphToUpper(column.getName()))
          .returns(_className).addModifiers(Modifier.PUBLIC).addParameter(javaTypeClassName, column.getName())
          .addStatement("this.$L = $L", column.getName(), column.getName()).addStatement("return this")
          .addJavadoc("$L", column.getNote()).build());
    }

    try {
      JavaFile
          .builder(config.getBeanInfo().getPackageName(), dtoTypeBuilder.build())
          .build()
          .writeTo(
              new File(new File(config.getOutputLocation()).getPath() + "\\" + config.getBeanInfo().getFolderName()));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
