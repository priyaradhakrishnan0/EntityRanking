package Version2;
/*Original code from http://homepage.cs.uiowa.edu/~sriram/*/
import java.util.ArrayList;

////////////////////////////////////////////////////////////////
class StackX
{
	private final int SIZE = 30;
	private int[] st;
	private int top;
	//------------------------------------------------------------
	public StackX()           // constructor
	{
		st = new int[SIZE];    // make array
		top = -1;
	}
	//------------------------------------------------------------
	public void push(int j)   // put item on stack
	{ st[++top] = j; }
	//------------------------------------------------------------
	public int pop()          // take item off stack
	{ return st[top--]; }
	//------------------------------------------------------------
	public int peek()         // peek at top of stack
	{ return st[top]; }
	//------------------------------------------------------------
	public boolean isEmpty()  // true if nothing on stack
	{ return (top == -1); }
	//------------------------------------------------------------
}  // end class StackX
////////////////////////////////////////////////////////////////
class Vertex
{
	public String label;        // label (e.g. 'A')
	public boolean wasVisited;
	//------------------------------------------------------------
	public Vertex(String lab)   // constructor
	{
		label = lab;
		wasVisited = false;
	}
	//------------------------------------------------------------
}  // end class Vertex
////////////////////////////////////////////////////////////////
class Graph
{
	private final int MAX_VERTS = 30;
	private Vertex vertexList[]; // list of vertices
	private int adjMat[][];      // adjacency matrix
	private int nVerts;          // current number of vertices
	private StackX theStack;
	//------------------------------------------------------------
	public Graph()               // constructor
	{
		vertexList = new Vertex[MAX_VERTS];
		// adjacency matrix
		adjMat = new int[MAX_VERTS][MAX_VERTS];
		nVerts = 0;
		for(int y=0; y<MAX_VERTS; y++)      // set adjacency
			for(int x=0; x<MAX_VERTS; x++)   //    matrix to 0
				adjMat[x][y] = 0;
		theStack = new StackX();
	}  // end constructor
	//------------------------------------------------------------
	public void addVertex(String lab)
	{
		vertexList[nVerts++] = new Vertex(lab);
	}
	//------------------------------------------------------------
	public void addEdge(int start, int end)
	{
		adjMat[start][end] = 1;
		adjMat[end][start] = 1;
	}
	//------------------------------------------------------------
	public void displayVertex(int v)
	{
		System.out.print(" "+vertexList[v].label);
	}
	//------------------------------------------------------------
	public ArrayList<String> dfs()  // depth-first search
	{                                 // begin at vertex 0
		ArrayList<String> returnVertex = new ArrayList<String>(); 
		vertexList[0].wasVisited = true;  // mark it
		displayVertex(0);                 // display it
		theStack.push(0);                 // push it

		while( !theStack.isEmpty() )      // until stack empty,
		{
			// get an unvisited vertex adjacent to stack top
			int v = getAdjUnvisitedVertex( theStack.peek() );
			if(v == -1)                    // if no such vertex,
				theStack.pop();
			else                           // if it exists,
			{
				vertexList[v].wasVisited = true;  // mark it
				displayVertex(v);                 // display it
				returnVertex.add(vertexList[v].label);
				theStack.push(v);                 // push it
			}
		}  // end while

		// stack is empty, so we're done
		for(int j=0; j<nVerts; j++)          // reset flags
			vertexList[j].wasVisited = false;
		return returnVertex;
	}  // end dfs
	//------------------------------------------------------------
	// returns an unvisited vertex adj to v
	public int getAdjUnvisitedVertex(int v)
	{
		for(int j=0; j<nVerts; j++)
			if(adjMat[v][j]==1 && vertexList[j].wasVisited==false)
				return j;
		return -1;
	}  // end getAdjUnvisitedVertex()
	//------------------------------------------------------------
}  // end class Graph
////////////////////////////////////////////////////////////////
public class DepthFirstSearch 
{
	public static void main(String[] args)
	{
		Graph theGraph = new Graph();
		theGraph.addVertex("A");    // 0  (start for dfs)
		theGraph.addVertex("B");    // 1
		theGraph.addVertex("C");    // 2
		theGraph.addVertex("D");    // 3
		theGraph.addVertex("E");    // 4

		theGraph.addEdge(0, 1);     // AB
		theGraph.addEdge(1, 2);     // BC
		theGraph.addEdge(0, 3);     // AD
		theGraph.addEdge(3, 4);     // DE

		System.out.print("Visits: ");
		theGraph.dfs();             // depth-first search
		System.out.println();
	}  // end main()
    public int[] bubbleSort(int array[]) {
        int n, c, d, swap;
        n = array.length;
     
        for (c = 0; c < ( n - 1 ); c++) {
          for (d = 0; d < n - c - 1; d++) {
            if (array[d] < array[d+1]) /* Ascending order */
            {
              swap       = array[d];
              array[d]   = array[d+1];
              array[d+1] = swap;
            }
          }
        }
     
        System.out.println("Sorted list of numbers :");
     
        for (c = 0; c < n; c++) 
          System.out.print(array[c]);
        return array;
      }
}  // end class DFS
////////////////////////////////////////////////////////////////