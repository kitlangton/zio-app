tmux split -f -p 80 "sbt '~examplesJVM/reStart'"
tmux split -f -p 80 "sleep 2; sbt '~examplesJS/fastLinkJS'"
tmux attach