package ai;

import eval.BonusEvaluator;
import model.AbstractState;
import model.State;

import java.util.*;

import model.AbstractState.MOVE;

public class JamesConway2048 extends AbstractPlayer {

    //TODO
    //TODO I have changed a line in controller class

    private Random rng = new Random();
    int turn = 0;
    HashMap<Integer, MOVE> moveOrder;
    HashMap<Integer, Integer> score;
    ArrayList<TreeNode> fullTree;
    BonusEvaluator evaluator;
    Long start;
    int time;

    @Override
    public MOVE getMove(State game) {
        // Delay for the view
        pause();

        start = System.currentTimeMillis();

        evaluator = new BonusEvaluator();

        fullTree = new ArrayList<>();

        State gameCopy = game.copy();
        monteCarlo(gameCopy);

        if(turn == 100){
            System.out.print("");
        }

        turn++;

        return moveOrder.get(0);

    }

    private void monteCarlo(State game){
        TreeNode currentNode = new TreeNode(game);

        for(int i = 0; i< 180; i++) {
            time = (int) (System.currentTimeMillis() - start);
            if(time >= 75) {
                findBestRoute(getInitialNode(currentNode));
                return;
            }
            if (isLeaf(currentNode)) {
                //if currentNode has never been visited
                if (currentNode.getN() == 0) {
                    rollout(currentNode);
                } else {
                    //if node has been visited
                    ArrayList<TreeNode> children = createChildren(currentNode);
                    for (TreeNode child : children) {
                        currentNode.addChild(child);
                    }
                    if(children.size()>0) {
                        //if state is not terminal
                        currentNode = currentNode.getChildren().get(0);
                        rollout(currentNode);
                    }else{
                        //state is terminal
                        backPropegate(currentNode, (int)evaluator.evaluate(currentNode.getData().copy()));
                    }
                }
                currentNode = getInitialNode(currentNode);
            } else {
                //Make current node the best child
                currentNode = maxUCB(currentNode);
                //System.out.println(UCB(currentNode) + " " + currentNode.getN());
            }
        }
       findBestRoute(getInitialNode(currentNode));
    }

    private TreeNode rollout(TreeNode tn){
        State state = tn.getData().copy();
        int totalScore = 0, averageScore = 0, minScore = Integer.MAX_VALUE, maxScore =0, maxTile = 0;
        int timesRun = 35;
        //use these to keep time <= 80
        if(state.getHighestTileValue()<600)
            timesRun = 25;
        if(state.getHighestTileValue()<200)
            timesRun = 15;
        for(int i=1; i<timesRun; i++) {
            while (state.getMoves().size() > 0) {
                state.move(getRandomMove(state));
            }
            if(state.getScore()<minScore)
                minScore = state.getScore();
            if(state.getScore()>maxScore)
                maxScore = state.getScore();
            if(state.getHighestTileValue()>maxTile)
                maxTile = state.getHighestTileValue();
            totalScore += state.getScore();
            state = tn.getData().copy();
        }

        averageScore = (totalScore /timesRun);

        tn.setN(1);
        //tn.setT(getValue(state, tn));
        //TODO if the state has no moves set t very low
        tn.setT((int)evaluator.evaluate(tn.getData().copy())+(int)(averageScore/1.5));
        /*if(tn.getData().copy().getMoves().size() == 0){
            tn.setT((int)(tn.getT()/1.2));
        }*/
        backPropegate(tn, tn.getT());
        return tn;
    }

    private void backPropegate(TreeNode tn, int t){
        TreeNode parent = tn.getParent();
        if(parent != null) {
            parent.setN(parent.getN() + 1);
            parent.setT(parent.getT() + t);
            backPropegate(parent, t);
        }
    }

    private int getValue(State state, TreeNode tn){
        int value;
        State currentState = tn.getData();
        value = state.getScore() + (state.getScore()/40)*currentState.getNumberOfEmptyCells();

       if(hasDuplicates(currentState)){
            value*= 1.2;
       }

       //TODO check adjacent for same value

        //TODO check if highest score is in top left corner
        if(currentState.getBoardArray()[0][0] == currentState.getHighestTileValue()){
            System.out.println("bamm");
            value*=1.2;
        }

        return value;
    }

    private boolean isLeaf(TreeNode node){
        return node.children.size()==0;
    }

    private ArrayList<TreeNode> createChildren(TreeNode tn){
        State state = tn.getData();
        ArrayList<TreeNode> children = new ArrayList<>();
        for(MOVE move: state.getMoves()){
            state = tn.getData().copy();
            state.move(move);
            TreeNode tn1 = new TreeNode(state);
            tn1.setMove(move);
            tn1.setScore(state.getScore() - tn.getData().getScore());
            children.add(tn1);
        }
        return children;
    }

    private void findBestRoute(TreeNode tn){
        //Getting best route
        moveOrder = new HashMap<>();
        score = new HashMap<>();
        int i =0;
        int max;
        while(!isLeaf(tn)){
            max = -1000;
            //find max t from children
            for(TreeNode child:tn.getChildren()){
                if(child.getUcb()>max){
                    max = child.getUcb();
                    tn = child;
                }
            }
            moveOrder.put(i, tn.getMove());
            score.put(i, tn.getScore());
            i++;
        }

    }

    public boolean hasDuplicates(State state){
        int[][] board = state.getBoardArray();
        HashMap<Integer, Integer> duplicates = new HashMap<>();

        for(int i=0; i<4; i++){
            for(int j=0;j<4;j++){
                if(duplicates.get(board[i][j])!=null) {
                    duplicates.put(board[i][j], duplicates.get(board[i][j]) + 1);
                }else{
                    duplicates.put(board[i][j], 1);
                }
            }
        }

        Iterator it = duplicates.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if((int)pair.getValue()>12){
                return true;
            }
        }
        return false;
    }

    private TreeNode getInitialNode(TreeNode tn){
        TreeNode parent = tn;
        while(true) {
            if(parent.getParent() == null) {
                return parent;
            }
            parent = parent.getParent();
        }
    }

    private int UCB(TreeNode tn){
        //if top node or never visited
        if(tn.getN() == 0 || tn.getParent() == null){
            return Integer.MAX_VALUE;
        }

        if(tn.getT()<0){
            return -1;
        }

        return (int)((tn.getT()/tn.getN()) + 4 * Math.sqrt(Math.log(getInitialNode(tn).getN())/tn.getN()));
    }

    private TreeNode maxUCB(TreeNode tn){
        int maxUCB = Integer.MIN_VALUE;
        for(TreeNode child:tn.children){
            if(UCB(child)>maxUCB){
                tn = child;
                maxUCB = UCB(child);
            }
        }
        return tn;
    }

    private MOVE getRandomMove(State state){
        int random = (int) (Math.random() * state.getMoves().size());
        return state.getMoves().get(random);
    }

    private MOVE getHighestMove(State game){
        // Get available moves
        List<AbstractState.MOVE> moves = game.getMoves();

        HashMap<String, Integer> scores = new HashMap<>();

        for(int i =0; i<moves.size();i++){
            game.move(moves.get(i));
            scores.put("Move"+i, game.getScore());
            game.undo();
        }

        int max = 0;
        int bestMove = 0;

        for(int i = 0; i<scores.size(); i++){
            if(scores.get("Move"+i)>max){
                max = scores.get("Move" + i);
                bestMove = i;
            }
        }
        // Pick a move at random
        return moves.get(bestMove);
    }

    @Override
    public int studentID() {
        return 201803292;
    }

    @Override
    public String studentName() {
        return "James Conway";
    }

    public class TreeNode{

        private State data;
        private TreeNode parent;
        private ArrayList<TreeNode> children;
        private int n =0, t =0, score = 0, ucb;
        private MOVE move;


        public TreeNode(State data) {
            this.data = data;
            this.children = new ArrayList<>();
            fullTree.add(this);
            ucb = UCB(this);
        }

        public void addChild(TreeNode childNode) {
            childNode.parent = this;
            this.children.add(childNode);
        }

        public State getData() {
            return data;
        }

        public TreeNode getParent() {
            return parent;
        }

        public ArrayList<TreeNode> getChildren() {
            return children;
        }

        public int getN() {
            return n;
        }

        public void setN(int n) {
            this.n = n;
            ucb = UCB(this);
        }

        public int getT() {
            return t;
        }

        public void setT(int t) {
            this.t = t;
        }

        public int getUcb() {
            return  UCB(this);
        }

        public void setUcb(int ucb) {
            this.ucb = ucb;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public void setMove(MOVE move) {
            this.move = move;
        }

        public MOVE getMove() {
            return move;
        }

        public String toString(){
            return "N-" + n + " T-" + t + " UCB-" + ucb + " Score-" + score + " Move-" + move + " NumberOfChildren-" + children.size();
        }
    }

}

