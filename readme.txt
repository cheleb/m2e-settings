Usage example:

<build>
		<plugins>
			<plugin>
				<groupId>org.eclipse.m2e.settings</groupId>
				<artifactId>maven-eclipse-plugin</artifactId>
				<version>1.1.1</version>
				<configuration>
					<formatter>
						<filename>sample-formatting.xml</filename>
					</formatter>
				</configuration>
				<executions>
					<execution>
						<id></id>
						<goals>
							<goal>eclipse</goal>
						</goals>
						<inherited>false</inherited>

					</execution>

				</executions>
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
