FROM maven:3.8-openjdk-11

# Copy the spring-boot-openshift-interop-tests repository into /spring-boot-openshift-interop-tests
RUN mkdir /spring-boot-openshift-interop-tests
WORKDIR /spring-boot-openshift-interop-tests
COPY . .

# Create required directories
RUN mkdir /.m2

# Add required permissions for OpenShift
RUN chgrp -R 0 /spring-boot-openshift-interop-tests && \
    chmod -R g=u /spring-boot-openshift-interop-tests && \
    chgrp -R 0 /.m2 && \
    chmod -R g=u /.m2

CMD ["/bin/bash"]