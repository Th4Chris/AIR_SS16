package infoRetrieval;

public class Topic {
	private int id;
	private String title;
	private String description;
	private String narrative;
	
	public Topic(int id, String title, String description, String narrative){
		this.id=id;
		this.title=title;
		this.description=description;
		this.narrative=narrative;
	}
	
	public int getId(){
		return id;
	}
	public String getTitle(){
		return title;
	}
	public String getDescription(){
		return description;
	}
	public String getNarrative(){
		return narrative;
	}
	public String toString(){
		return "Topic id: "+id+" title: "+title+" desc: "+description+" narr: "+narrative;
	}
	

}
