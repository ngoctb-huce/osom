FROM ibm-semeru-runtimes:open-17.0.7_7-jdk
MAINTAINER openslice.io
RUN mkdir /opt/shareclasses
RUN mkdir -p /opt/openslice/lib/
COPY target/org.etsi.osl.osom-1.1.0.jar /opt/openslice/lib/
COPY target/org.etsi.osl.osom-1.1.0-exec.jar /opt/openslice/lib/
COPY . /opt/openslice/lib/
CMD ["java", "-Xshareclasses:cacheDir=/opt/shareclasses","-jar", "/opt/openslice/lib/org.etsi.osl.osom-1.1.0-exec.jar"]