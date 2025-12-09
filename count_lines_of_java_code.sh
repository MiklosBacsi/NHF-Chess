find . -name "*.java" ! -path "*/target/*" ! -path "*/build/*" | xargs wc -l
