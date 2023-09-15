import json
import websocket
from pathlib import Path
import base64
import uuid
import datetime
import re

websocket_uri = "ws://localhost:3000/ws"

def on_message(ws, message):
    if "ucs-command" in message:
        print("\nReceived message from the ucs:")
        parsed = json.loads(message)
        print(json.dumps(parsed, indent=2))
        print("\n--------------------------------\n")

        if parsed["Volatile"]["value"]["command"]["value"]["message"]["code"] == "OK":
            exit("Policy correctly uploaded.")
        elif parsed["Volatile"]["value"]["command"]["value"]["message"]["code"] == "KO":
            exit("[ERROR] Policy was not accepted by the UCS.")
        else:
            exit("The received message is not intended for this PAP")

def on_error(ws, error):
    print(error)

def on_close(ws, close_status_code, close_msg):
    print("### Connection closed ###")

def on_open(ws):
    print("### Connection established ###")
    print("[ " + websocket_uri + " ]")
    add_policy()

def print_and_send(json_out):
    print("Message sent:")
    print(json.dumps(json_out, indent=2))
    ws.send(json.dumps(json_out))

## ADD POLICY
def add_policy():
    print("")
    filename = input("Enter policy file name> ")
    try:
        policy = Path(filename).read_text()
    except Exception:
        exit("File does not exist")
    policy_id_from_filename = Path(filename).stem

    # Define the regex pattern to match the text between "PolicyId=" and the next "
    pattern = r'PolicyId="([^"]+)"'

    # Use re.search to find the first match
    match = re.search(pattern, policy)

    # Check if a match was found
    if match:
        policy_id = match.group(1)
        print("PolicyId: ", policy_id)
    else:
        exit("No PolicyId found")

    if policy_id != policy_id_from_filename:
        print("[WARNING] The PolicyId found in the file differs from the filename.")
        print("          If you proceed, the PolicyId found in the file will be used,")
        print("          and the UCS will save the policy with the name " + policy_id + ".xml")
        answer = "?"
        while answer != "y" and answer != "n":
            answer = input("\nDo you want to proceed anyway? (y/n)> ")
            if answer == "y":
                continue
            elif answer == "n":
                exit("")

    b = base64.b64encode(bytes(policy, 'utf-8')) # bytes
    policy64 = b.decode('utf-8')

    ws_req = {
        "RequestPubMessage": {
            "value": {
                "timestamp": int(datetime.datetime.now().timestamp()*1000),
                "command": {
                    "command_type": "pap-command",
                    "value": {
                        "message": {
                            "purpose": "ADD_POLICY",
                            "message_id": str(uuid.uuid1()),
                            "policy": policy64,
                            "policy_id": policy_id
                        },
                        "id": "pap-web_socket",
                        "topic_name": "topic-name",
                        "topic_uuid": "topic-uuid-the-ucs-is-subscribed-to"
                    }
                }
            }
        }
    }
    print("\n--------- ADD POLICY -----------\n")
    print_and_send(ws_req)

if __name__ == "__main__":
    ws = websocket.WebSocketApp(websocket_uri,
                                on_open=on_open,
                                on_message=on_message,
                                on_error=on_error,
                                on_close=on_close)

    ws.run_forever()
