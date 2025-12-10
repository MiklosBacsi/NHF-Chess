echo "Lines of Java code with comments:"
find . -name "*.java" ! -path "*/target/*" ! -path "*/build/*" | xargs wc -l
