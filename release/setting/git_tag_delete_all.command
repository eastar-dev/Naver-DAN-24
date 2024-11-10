cd $(dirname $0)
git tag -d $(git tag --list '[a-z]*')
git ls-remote --tags origin --list 'refs/tags/[a-z]*' | awk '!/(})/ { print ":"$2 }' | xargs git push origin
#read -s -n1 -p "Press any key to continue...." keypress
#exit 0
