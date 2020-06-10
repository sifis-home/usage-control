package it.cnr.iit.ucs.pipdsa.types;

public class Message {
	String status;
	String message;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "status: " + status + ", message: " + message;
	}

}
