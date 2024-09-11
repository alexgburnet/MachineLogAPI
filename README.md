# Machine Operations and Fault Logs API

## Project Overview

This project provides an API for managing machine operations, including corrective actions, linear thread status, accountable knitters, and various fault logs. It includes methods for querying and updating machine data, assigning operators, and recording faults and production logs.

## Prerequisites

- Java 11 or higher
- PostgreSQL database
- JDBC driver for PostgreSQL

## Setup

1. **Clone the Repository**

```bash
git clone https://github.com/alexgburnet/MachineLogAPI.git
cd MachineLogAPI
```

2. **Configure Database**

Update the database configuration details in your code. Set the dbURL, username, and password to match your PostgreSQL database settings.
Add a config.properties file to root:

```config
psql.username=<your_user>
psql.password=<your_password>
```

3. **Compile and run**

Compile and run using Gradle. Ensure that the JDBC driver is included in the build config.

## Database Structure:

<p align="center">
  <img src="https://github.com/alexgburnet/MachineLogAPI/blob/master/assets/Machine%20Log%20API%20ERD.png" alt="learning page" width="700"/>
</p>

Example:
```sql
CREATE TABLE fault_codes (
    code INT PRIMARY KEY,
    description TEXT NOT NULL
);

CREATE TABLE operators (
    code INT PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE faults (
    id SERIAL PRIMARY KEY,
    date TIMESTAMP NOT NULL,
    fault_code INT REFERENCES fault_codes(code),
    operator_code INT REFERENCES operators(code),
    fault_time INTERVAL NOT NULL,
    machine_number INT NOT NULL,
    visible BOOLEAN DEFAULT TRUE
);

CREATE TABLE accountable_knitter (
	id serial PRIMARY KEY,
	date TIMESTAMP NOT NULL,
	shift TEXT NOT NULL,
	machine_number INT NOT NULL,
	operator INT NOT NULL
);

CREATE TABLE corrective_actions (
    id SERIAL PRIMARY KEY,
    date TIMESTAMP NOT NULL,
    machine_number INT NOT NULL,
    isDayShift BOOLEAN NOT NULL,
    isLinearThread BOOLEAN NOT NULL,
    fault_code INT REFERENCES fault_codes(code),
    observation TEXT,
    action TEXT,
	date_completed TIMESTAMP
);

CREATE TABLE linear_thread (
    id SERIAL PRIMARY KEY,
    date TIMESTAMP NOT NULL,
    machine_number INT NOT NULL,
    isDayShift BOOLEAN NOT NULL,
    islinearthread BOOLEAN DEFAULT false
)
```

## API Endpoints

### `GET /api/corrective-action`

**Description**: Retrieves corrective actions for a given machine number, date, and shift.

**Inputs**:

- `date`: The date in the format "yyyy-MM-dd".
- `machineNumber`: The machine number.
- `isDayShift`: Boolean indicating whether it is a day shift.
- `fault`: The fault description.

**Returns**: A JSON object with keys "observation" and "action", or an error message if no data is found.

### `GET /api/linear-thread`

**Description**: Retrieves the linear thread status for a given machine number, date, and shift.

**Inputs**:

- `date`: The date in the format "yyyy-MM-dd".
- `machineNumber`: The machine number.
- `isDayShift`: Boolean indicating whether it is a day shift.

**Returns**: A boolean indicating if the linear thread is active or not.

### `POST /api/linear-thread`

**Description**: Sets the linear thread status for a given machine number, date, and shift. Replaces any existing entry.

**Inputs**:

- `date`: The date in the format "yyyy-MM-dd".
- `machineNumber`: The machine number.
- `isDayShift`: Boolean indicating whether it is a day shift.
- `isLinearThread`: Boolean indicating the status of the linear thread.

**Returns**: Success or error message.

### `GET /api/action-list`

**Description**: Retrieves a list of incomplete corrective actions, ordered by date.

**Returns**: A JSON object containing a list of corrective actions with their details.

### `POST /api/complete-action`

**Description**: Marks a corrective action as completed.

**Inputs**:

- `id`: The ID of the corrective action.
- `date`: The completion date in the format "yyyy-MM-dd HH:mm:ss".

**Returns**: Success or error message.

### `GET /api/operators`

**Description**: Retrieves a list of operators with their codes and names.

**Returns**: A JSON object where the key is the operator code and the value is the operator name.

### `GET /api/accountable-knitter`

**Description**: Checks if an accountable knitter is assigned to machines for a given date and shift.

**Inputs**:

- `date`: The date in the format "yyyy-MM-dd".
- `shift`: The shift ("day" or "night").
- `machines`: A list of machine numbers.

**Returns**: A JSON object where the key is the machine number and the value is the knitter’s name, or -1 and "Unassigned" if no knitter is assigned.

### `POST /api/accountable-knitter`

**Description**: Sets an accountable knitter for the given machines, replacing any existing assignments.

**Inputs**:

- `operator`: The operator code.
- `date`: The date in the format "yyyy-MM-dd".
- `shift`: The shift ("day" or "night").
- `machines`: A list of machine numbers.

**Returns**: Success or error message.

### `POST /api/knitting-fault-log`

**Description**: Inserts a knitting fault log into the database.

**Inputs**:

- `data`: The fault log data in the format "dd/MM/yyyy hh:mm:ss;fault_code;fault_description;operator_code;operator_name;fault_time;machine_number".

**Returns**: Success or error message.

### `POST /api/remove-fault`

**Description**: Marks a fault as not visible, effectively removing it from the visible records.

**Inputs**:

- `ID`: The ID of the fault to be removed.

**Returns**: Success or error message.

### `POST /api/warping-fault-log`

**Description**: Placeholder method for inserting a warping fault log into the database.

**Inputs**:

- `data`: The fault log data.

**Returns**: Success or error message.

### `POST /api/knitting-production-log`

**Description**: Placeholder method for inserting a knitting production log into the database.

**Inputs**:

- `data`: The production log data.

**Returns**: Success or error message.

### `POST /api/warping-production-log`

**Description**: Placeholder method for inserting a warping production log into the database.

**Inputs**:

- `data`: The production log data.

**Returns**: Success or error message.

### `POST /api/knitting-warp-ref-log`

**Description**: Placeholder method for inserting a knitting warp reference log into the database.

**Inputs**:

- `data`: The warp reference log data.

**Returns**: Success or error message.
