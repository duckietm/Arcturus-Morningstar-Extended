FROM maven:latest AS builder

# Copy the Emulator sources to the container
COPY . .
# Package it
RUN mvn package && mv /target/Habbo*-with-dependencies.jar /target/Habbo.jar

# Use Java 8 for running
FROM java:8 AS runner

# Copy the generated source
COPY --from=builder /target/Habbo.jar /

# Save the script to wait for the database, among running the Arcturus Emulator
RUN echo "#!/bin/bash \n java -Dfile.encoding=UTF-8 -jar /Habbo.jar" > /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Run the Emulator with Java
ENTRYPOINT ["/entrypoint.sh"]
