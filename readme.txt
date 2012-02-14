Usage example:

<build>
  <plugins>
    <plugin>
      <groupId>org.eclipse.m2e.settings</groupId>
      <artifactId>maven-eclipse-plugin</artifactId>
      <version>1.2.1</version>

      <configuration>
        <formatter>
          <filename>sample-formatting.xml</filename>
        </formatter>
      </configuration>

      <dependencies>
     
       <dependency>
        <groupId>org.eclipse.m2e.settings</groupId>
        <artifactId>sample-configuration-artifact</artifactId>
        <version>1.0.0</version>
       </dependency>
      
     </dependencies>
   </plugin>
 </plugins>
</build>



